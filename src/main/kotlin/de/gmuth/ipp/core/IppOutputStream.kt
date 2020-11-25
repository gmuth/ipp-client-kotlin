package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.log.Log
import java.io.DataOutputStream
import java.io.OutputStream
import java.net.URI
import java.nio.charset.Charset

class IppOutputStream(outputStream: OutputStream) : DataOutputStream(outputStream) {

    companion object {
        val log = Log.getWriter("IppOutputStream", Log.Level.WARN)
    }

    // charset for text and name attributes, rfc 8011 4.1.4.1
    private var operationAttributesCharset: Charset? = null

    fun writeMessage(message: IppMessage) {
        operationAttributesCharset = message.operationGroup.attributesCharset
        with(message) {
            writeVersion(version ?: throw IppException("missing version"))
            log.trace { "version = $version" }

            writeShort(code?.toInt() ?: throw IppException("missing operation or status code"))
            log.trace { "code = $code ($codeDescription)" }

            writeInt(requestId ?: throw IppException("missing requestId"))
            log.trace { "requestId = $requestId" }

            for (group in attributesGroups) {
                writeTag(group.tag)
                for (attribute in group.values) {
                    try {
                        writeAttribute(attribute)
                    } catch (exception: Exception) {
                        throw IppException("failed to write attribute: $attribute", exception)
                    }
                }
            }

            writeTag(IppTag.End)
        }
    }

    private fun writeVersion(version: IppVersion) {
        with(version) {
            writeByte(major)
            writeByte(minor)
        }
    }

    private fun writeAttribute(attribute: IppAttribute<*>) {
        log.trace { "$attribute" }
        with(attribute) {
            attribute.checkSyntax()
            if (tag.isOutOfBandTag() || tag == IppTag.EndCollection) {
                assertNoValues()
                writeTag(tag)
                writeString(name)
                writeShort(0) // no value
            } else {
                assertValuesExist()
                // iterate 1setOf
                for ((index, value) in values.withIndex()) {
                    writeTag(tag)
                    writeString(if (index == 0) name else "")
                    writeAttributeValue(tag, value!!)
                }
            }
        }
    }

    private fun writeTag(tag: IppTag) {
        if (tag.isDelimiterTag()) log.trace { "--- $tag ---" }
        writeByte(tag.code.toInt())
    }

    private fun writeString(string: String, charset: Charset = Charsets.US_ASCII) {
        with(string.toByteArray(charset)) {
            writeShort(size)
            write(this)
        }
    }

    private fun writeAttributeValue(tag: IppTag, value: Any) {

        fun writeString(value: String) = writeString(value, tag.selectCharset(operationAttributesCharset!!))

        when (tag) {

            IppTag.Boolean -> with(value as Boolean) {
                writeShort(1)
                writeByte(if (value) 0x01 else 0x00)
            }

            IppTag.Integer,
            IppTag.Enum -> with(value as Int) {
                writeShort(4)
                writeInt(value)
            }

            IppTag.RangeOfInteger -> with(value as IntRange) {
                writeShort(8)
                writeInt(start)
                writeInt(endInclusive)
            }

            IppTag.Resolution -> with(value as IppResolution) {
                writeShort(9)
                writeInt(x)
                writeInt(y)
                writeByte(unit)
            }

            IppTag.Charset -> with(value as Charset) {
                writeString(value.name().toLowerCase())
            }

            IppTag.Uri -> with(value as URI) {
                writeString(value.toString())
            }

            IppTag.Keyword,
            IppTag.UriScheme,
            IppTag.OctetString,
            IppTag.MimeMediaType,
            IppTag.MemberAttrName,
            IppTag.NaturalLanguage -> with(value as String) {
                writeString(value)
            }

            IppTag.TextWithoutLanguage,
            IppTag.NameWithoutLanguage -> when {
                (value is IppString) -> writeString(value.text)
                (value is String) -> writeString(value) // accept String for convenience
                else -> throw IppException("expected value class IppString without language or String")
            }

            IppTag.TextWithLanguage,
            IppTag.NameWithLanguage -> with(value as IppString) {
                writeShort(4 + text.length + language!!.length)
                writeString(value.language!!)
                writeString(value.text)
            }

            IppTag.DateTime -> with(value as IppDateTime) {
                writeShort(11)
                writeShort(year)
                writeByte(month)
                writeByte(day)
                writeByte(hour)
                writeByte(minutes)
                writeByte(seconds)
                writeByte(deciSeconds)
                writeByte(directionFromUTC.toInt())
                writeByte(hoursFromUTC)
                writeByte(minutesFromUTC)
            }

            IppTag.BegCollection -> with(value as IppCollection) {
                writeShort(0)
                for (member in members) {
                    writeAttribute(IppAttribute("", IppTag.MemberAttrName, member.name))
                    for (memberValue in member.values) {
                        writeAttribute(IppAttribute("", member.tag, memberValue))
                    }
                }
                writeAttribute(IppAttribute<Unit>("", IppTag.EndCollection))
            }

            else -> {
                throw IppException(String.format("unable to encode tag %s (%02X)", tag, tag.code))
            }
        }
    }
}