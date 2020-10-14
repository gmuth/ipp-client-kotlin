package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.iana.IppRegistrationsSection2
import java.io.DataOutputStream
import java.io.OutputStream
import java.net.URI
import java.nio.charset.Charset
import java.util.*

class IppOutputStream(outputStream: OutputStream) : DataOutputStream(outputStream) {

    companion object {
        var checkSyntax: Boolean = true
    }

    // charset for text and name attributes, rfc 8011 4.1.4.1
    private var attributesCharset: Charset? = null

    fun writeMessage(message: IppMessage) {
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

    private fun writeVersion(version: String) {
        val matchResult = """^(\d)\.(\d)$""".toRegex().find(version)
        if (matchResult == null) {
            throw IppException("invalid version string: '$version'")
        } else with(matchResult) {
            val major: Int = groups[1]!!.value.toInt()
            val minor: Int = groups[2]!!.value.toInt()
            writeByte(major)
            writeByte(minor)
        }
    }

    private fun writeAttribute(attribute: IppAttribute<*>) {
        with(attribute) {
            if (checkSyntax) {
                IppRegistrationsSection2.checkSyntaxOfAttribute(name, tag)
            }
            if (tag.isOutOfBandTag() || tag == IppTag.EndCollection) {
                if (values.isNotEmpty()) {
                    throw IppException("'$name' must not have any value")
                }
                writeTag(tag)
                writeString(name, Charsets.US_ASCII)
                writeShort(0) // no value

            } else {
                if (values.isEmpty()) {
                    throw IppException("no values found to write for '$name'")
                }
                // 1setOf iteration
                for ((index, value) in values.withIndex()) {
                    writeTag(tag)
                    writeString(if (index == 0) name else "", Charsets.US_ASCII)
                    writeAttributeValue(tag, value!!)
                }
                if (values.size > 1 && IppRegistrationsSection2.attributeIs1setOf(name) == false) {
                    println("WARN: '$name' is not registered as '1setOf'")
                }
            }
            // keep attributes-charset for name and text value encoding
            if (tag == IppTag.Charset && name == "attributes-charset") {
                attributesCharset = value as Charset
            }
        }
    }

    private fun writeTag(tag: IppTag) = writeByte(tag.code.toInt())

    private fun writeAttributeValue(tag: IppTag, value: Any) {

        fun writeStringForTag(value: String) {
            val charset = if (tag.useAttributesCharset()) {
                attributesCharset ?: throw IppException("missing attributes-charset in IppMessage")
            } else {
                Charsets.US_ASCII
            }
            writeString(value, charset)
        }

        if (tag.isOutOfBandTag() || tag == IppTag.EndCollection) {
            throw IppException("tag '$tag' does not support any value")
        }
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
                writeStringForTag(value.name().toLowerCase())
            }

            IppTag.NaturalLanguage -> with(value as Locale) {
                writeStringForTag(value.toLanguageTag().toLowerCase())
            }

            IppTag.Uri -> with(value as URI) {
                writeStringForTag(value.toString())
            }

            IppTag.OctetString,
            IppTag.Keyword,
            IppTag.UriScheme,
            IppTag.MimeMediaType,
            IppTag.MemberAttrName -> with(value as String) {
                writeStringForTag(value)
            }

            IppTag.TextWithoutLanguage,
            IppTag.NameWithoutLanguage -> when {
                (value is IppString) -> writeStringForTag(value.string)
                (value is String) -> writeStringForTag(value) // accept String for convenience
                else -> throw IppException("expected value class IppString without language or String")
            }

            IppTag.TextWithLanguage,
            IppTag.NameWithLanguage -> with(value as IppString) {
                writeShort(4 + string.length + language?.length!!)
                writeStringForTag(value.language!!)
                writeStringForTag(value.string)
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
                throw IppException(String.format("tag %s (%02X) encoding not implemented", tag, tag.code))
            }
        }
    }

    private fun writeString(value: String, charset: Charset) {
        with(value.toByteArray(charset)) {
            writeShort(size)
            write(this)
        }
    }

}