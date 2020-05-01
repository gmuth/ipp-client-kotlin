package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.iana.IppRegistrationsSection2
import java.io.DataOutputStream
import java.io.OutputStream
import java.net.URI
import java.nio.charset.Charset

class IppOutputStream(outputStream: OutputStream) : DataOutputStream(outputStream) {

    companion object {
        var checkSyntax: Boolean = true
    }

    // charset for text and name attributes, rfc 8011 4.1.4.1
    private var attributesCharset: Charset? = null

    private fun charsetForTag(tag: IppTag) =
            if (tag.useAttributesCharset()) attributesCharset ?: throw IppException("missing attributes-charset")
            else Charsets.US_ASCII

    fun writeMessage(message: IppMessage) {
        with(message) {
            writeVersion(version ?: throw IppException("missing version"))
            writeShort(code?.toInt() ?: throw IppException("missing operation or status code"))
            writeInt(requestId ?: throw IppException("missing requestIds"))
            for (group in attributesGroups) {
                writeAttributesGroup(group)
            }
            writeTag(IppTag.End)
        }
    }

    private fun writeVersion(version: IppVersion) {
        writeByte(version.major)
        writeByte(version.minor)
    }

    private fun writeAttributesGroup(attributesGroup: IppAttributesGroup) {
        with(attributesGroup) {
            writeTag(tag)
            for (attribute in values) {
                try {
                    writeAttribute(attribute)
                } catch (exception: Exception) {
                    throw IppException("failed to write attribute: $attribute", exception)
                }
            }
        }
    }

    private fun writeAttribute(attribute: IppAttribute<*>) {
        with(attribute) {
            if (checkSyntax) {
                IppRegistrationsSection2.checkSyntaxOfAttribute(name, tag)
            }

            if (values.isEmpty()) {
                if (tag.isOutOfBandTag() || tag == IppTag.EndCollection) {
                    writeTag(tag)
                    writeString(name, Charsets.US_ASCII)
                    writeAttributeValue(tag, values)
                } else {
                    throw IppException("no values found to write for '$name'")
                }
            } else {
                // 1setOf iteration
                for ((index, value) in values.withIndex()) {
                    writeTag(tag)
                    writeString(if (index == 0) name else "", Charsets.US_ASCII)
                    writeAttributeValue(tag, value)
                }
                if (values.size > 1 && IppRegistrationsSection2.attributeIs1setOf(name) == false) {
                    println("WARN: '$name' is not registered as '1setOf'")
                }
            }
            // keep attributes-charset for name and text value encoding
            if (tag == IppTag.Charset && name == "attributes-charset") {
                attributesCharset = Charset.forName(value as String)
            }
        }
    }

    private fun writeTag(tag: IppTag) = writeByte(tag.code.toInt())

    private fun writeAttributeValue(tag: IppTag, value: Any?) {
        if (!tag.isOutOfBandTag() && tag != IppTag.EndCollection && value == null) {
            throw IppException("missing value for tag $tag")
        }
        when (tag) {
            // out-of-band RFC 8010 3.8. & RFC 3380 8.
            IppTag.Unsupported_,
            IppTag.Unknown,
            IppTag.NoValue,
            IppTag.NotSettable,
            IppTag.DeleteAttribute,
            IppTag.AdminDefine,
            IppTag.EndCollection -> writeShort(0)

            IppTag.Boolean -> with(value as Boolean) {
                writeShort(1)
                writeByte(if (value) 0x01 else 0x00)
            }

            IppTag.Integer,
            IppTag.Enum -> with(value as Int) {
                writeShort(4)
                writeInt(value)
            }

            IppTag.RangeOfInteger -> with(value as IppIntegerRange) {
                writeShort(8)
                writeInt(start)
                writeInt(end)
            }

            IppTag.Resolution -> with(value as IppResolution) {
                writeShort(9)
                writeInt(x)
                writeInt(y)
                writeByte(unit)
            }

            IppTag.Uri -> with(value as URI) {
                writeString(value.toString(), charsetForTag(tag))
            }

            IppTag.OctetString,
            IppTag.Keyword,
            IppTag.UriScheme,
            IppTag.Charset,
            IppTag.NaturalLanguage,
            IppTag.MimeMediaType,
            IppTag.MemberAttrName -> with(value as String) {
                writeString(value, charsetForTag(tag))
            }

            IppTag.TextWithoutLanguage,
            IppTag.NameWithoutLanguage -> when {
                (value is IppString) -> writeString(value.string, charsetForTag(tag))
                (value is String) -> writeString(value, charsetForTag(tag)) // accept String for convenience
                else -> throw IppException("expected value class IppString without language or String")
            }

            IppTag.TextWithLanguage,
            IppTag.NameWithLanguage -> with(value as IppString) {
                writeShort(4 + string.length + language?.length!!)
                writeString(value.language!!, charsetForTag(tag))
                writeString(value.string, charsetForTag(tag))
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
                    writeCollectionAttribute(IppTag.MemberAttrName, member.name)
                    for (value in member.values) {
                        writeCollectionAttribute(member.tag, value)
                    }
                }
                writeCollectionAttribute(IppTag.EndCollection)
            }

            else -> throw IppException(String.format("tag %s (%02X) encoding not implemented", tag, tag.code))
        }
    }

    private fun writeString(value: String, charset: Charset) {
        with(value.toByteArray(charset)) {
            writeShort(size)
            write(this)
        }
    }

    private fun writeCollectionAttribute(tag: IppTag, value: Any? = null) {
        writeAttribute(IppAttribute("", tag, value))
    }

}