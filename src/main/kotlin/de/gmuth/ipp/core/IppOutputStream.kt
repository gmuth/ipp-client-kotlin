package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.iana.IppRegistrations
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
            writeCode(code ?: throw IppException("missing operation or status code"))
            writeRequestId(requestId ?: throw IppException("missing requestIds"))
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

    private fun writeCode(code: Short) = writeShort(code.toInt())

    private fun writeRequestId(requestId: Int) = writeInt(requestId)

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
            if(checkSyntax) {
                IppRegistrations.checkSyntaxOfAttribute(name, tag)
            }
            if (tag != IppTag.NoValue && values.isEmpty()) {
                throw IppException("no values found to write for '$name'")
            }
            // 1setOf iteration
            for ((index, value) in values.withIndex()) {
                //println("write ${values.size.toPluralString("value")}: $name ($tag) = $values ")
                writeTag(tag)
                writeString(if (index == 0) name else "", Charsets.US_ASCII)
                writeAttributeValue(tag, value)
            }
            // keep attributes-charset for name and text value encoding
            if (tag == IppTag.Charset && name == "attributes-charset") {
                attributesCharset = Charset.forName(value as String)
            }
        }
    }

    private fun writeTag(tag: IppTag) = writeByte(tag.code.toInt())

    private fun writeAttributeValue(tag: IppTag, value: Any?) {
        if (value == null && !tag.isOutOfBandTag()) {
            throw IppException("missing value for tag $tag")
        }
        //tag.validateValueClass(value)
        when (tag) {
            // out-of-band RFC 8010 3.8. & RFC 3380 8.
            IppTag.Unsupported_,
            IppTag.Unknown,
            IppTag.NoValue,
            IppTag.NotSettable,
            IppTag.DeleteAttribute,
            IppTag.AdminDefine -> writeShort(0)

            // value class Boolean
            IppTag.Boolean -> with(value as Boolean) {
                writeShort(1)
                writeByte(if (value) 0x01 else 0x00)
            }

            // value class Int
            IppTag.Integer,
            IppTag.Enum -> with(value as Int) {
                writeShort(4)
                writeInt(value)
            }

            // value class IppIntegerRange
            IppTag.RangeOfInteger -> with(value as IppIntegerRange) {
                writeShort(8)
                writeInt(start)
                writeInt(end)
            }

            // value class IppResolution
            IppTag.Resolution -> with(value as IppResolution) {
                writeShort(9)
                writeInt(x)
                writeInt(y)
                writeByte(unit)
            }

            // value class URI
            IppTag.Uri -> with(value as URI) {
                writeString(value.toString(), charsetForTag(tag))
            }

            // value class String with rfc 8011 3.9 and rfc 8011 4.1.4.1 attribute value encoding
            IppTag.OctetString,
            IppTag.Keyword,
            IppTag.UriScheme,
            IppTag.Charset,
            IppTag.NaturalLanguage,
            IppTag.MimeMediaType -> with(value as String) {
                writeString(value, charsetForTag(tag))
            }

            // value class IppString
            IppTag.TextWithoutLanguage,
            IppTag.NameWithoutLanguage -> when {
                (value is IppString) -> writeString(value.string, charsetForTag(tag))
                (value is String) -> writeString(value, charsetForTag(tag)) // accept String for convenience
                else -> throw IppException("expected value class String or IppString without language")
            }

            IppTag.TextWithLanguage,
            IppTag.NameWithLanguage -> with(value as IppString) {
                writeShort(4 + string.length + language?.length!!)
                writeString(value.language ?: throw IppException("missing language"), charsetForTag(tag))
                writeString(value.string, charsetForTag(tag))
            }

            // value class IppDateTime
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

            else -> throw IppException(String.format("tag %s (%02X) encoding not implemented", tag, tag.code))
        }
    }

    private fun writeString(value: String, charset: Charset) {
        writeLengthAndValue(value.toByteArray(charset))
    }

    private fun writeLengthAndValue(value: ByteArray) {
        writeShort(value.size)
        write(value)
    }

}