package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.DataOutputStream
import java.io.OutputStream
import java.net.URI
import java.nio.charset.Charset
import java.util.*

class IppOutputStream(outputStream: OutputStream) : DataOutputStream(outputStream) {

    // charset for text and name attributes, rfc 8011 4.1.4.1
    private var messageAttributesCharset: Charset? = null

    fun writeMessage(message: IppMessage) {
        messageAttributesCharset = message.attributesCharset
        with(message) {
            writeVersion(version ?: throw IppException("missing version"))
            writeShort(code?.toInt() ?: throw IppException("missing operation or status code"))
            writeInt(requestId ?: throw IppException("missing requestId"))
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

    private fun writeTag(tag: IppTag) = writeByte(tag.code.toInt())

    private fun writeString(string: String, charset: Charset = Charsets.US_ASCII) {
        with(string.toByteArray(charset)) {
            writeShort(size)
            write(this)
        }
    }

    private fun writeAttributeValue(tag: IppTag, value: Any) {

        fun writeString(value: String) = writeString(value, tag.selectCharset(messageAttributesCharset!!))

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

            IppTag.NaturalLanguage -> with(value as Locale) {
                writeString(value.toLanguageTag().toLowerCase())
            }

            IppTag.Uri -> with(value as URI) {
                writeString(value.toString())
            }

            IppTag.OctetString,
            IppTag.Keyword,
            IppTag.UriScheme,
            IppTag.MimeMediaType,
            IppTag.MemberAttrName -> with(value as String) {
                writeString(value)
            }

            IppTag.TextWithoutLanguage,
            IppTag.NameWithoutLanguage -> when {
                (value is IppString) -> writeString(value.string)
                (value is String) -> writeString(value) // accept String for convenience
                else -> throw IppException("expected value class IppString without language or String")
            }

            IppTag.TextWithLanguage,
            IppTag.NameWithLanguage -> with(value as IppString) {
                writeShort(4 + string.length + language?.length!!)
                writeString(value.language!!)
                writeString(value.string)
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