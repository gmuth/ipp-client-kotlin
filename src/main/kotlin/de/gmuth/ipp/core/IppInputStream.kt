package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.Closeable
import java.io.DataInputStream
import java.io.InputStream
import java.net.URI
import java.nio.charset.Charset

class IppInputStream(inputStream: InputStream) : Closeable by inputStream {

    companion object {
        var compareTagsToIppRegistrations: Boolean = true
        var strict: Boolean = false
    }

    private val dataInputStream: DataInputStream = DataInputStream(inputStream)
    private var attributesCharset: Charset? = null // encoding for text and name attributes, rfc 8011 4.1.4.1

    private fun charsetForTag(tag: IppTag) =
            if (tag.useAttributesCharset()) attributesCharset ?: throw IppException("missing attributes-charset")
            else Charsets.US_ASCII

    fun readMessage(message: IppMessage) {
        with(message) {
            version = readVersion()
            code = readCode()
            requestId = readRequestId()
            lateinit var currentGroup: IppAttributesGroup
            lateinit var currentAttribute: IppAttribute<*>
            loop@ while (true) {
                val tag = readTag()
                when {
                    tag == IppTag.End -> break@loop
                    tag.isGroupTag() -> currentGroup = newAttributesGroup(tag)
                    else -> {
                        val attribute = readAttribute(tag)
                        if (attribute.name.isEmpty()) {
                            // found 1setOf value
                            with(currentAttribute) {
                                if (tag == attribute.tag) addValue(attribute.value)
                                else throw IppSpecViolation("'$name' 1setOf error: expected tag '$tag' but found '${attribute.tag}'")
                            }
                        } else {
                            currentAttribute = attribute
                            currentGroup.put(attribute)
                        }
                    }
                }
            }
        }
    }

    private fun readVersion() = with(dataInputStream) { IppVersion(read(), read()) }

    private fun readCode() = dataInputStream.readShort()

    private fun readRequestId() = dataInputStream.readInt()

    private fun readTag(): IppTag = IppTag.fromCode(dataInputStream.readByte())

    private fun readAttribute(tag: IppTag): IppAttribute<*> {
        val name = readString(Charsets.US_ASCII)
        var value = try {
            readAttributeValue(tag)
        } catch (exception: Exception) {
            throw IppException("failed to read attribute value for '$name' ($tag)", exception)
        }

        // check tag
        if (compareTagsToIppRegistrations) IppRegistrations.checkSyntaxOfAttribute(name, tag)
        //tag.validateValueClass(value)

        // keep attributes-charset for name and text value decoding
        if (name == "attributes-charset" && tag == IppTag.Charset) {
            attributesCharset = Charset.forName(value as String)
        }

        // move this somewhere else?
        if (!tag.isOutOfBandTag()) when (name) {
            "job-state" -> value = IppJobState.fromCode(value as Int)
        }

        return IppAttribute(name, tag, value)
    }

    private fun readAttributeValue(tag: IppTag): Any? = with(dataInputStream) {
        when (tag) {

            // out-of-band RFC 8010 3.8. & RFC 3380 8.
            IppTag.Unsupported_,
            IppTag.Unknown,
            IppTag.NoValue,
            IppTag.NotSettable,
            IppTag.DeleteAttribute,
            IppTag.AdminDefine -> {
                assertValueLength(0)
                null
            }

            // value class Boolean
            IppTag.Boolean -> {
                assertValueLength(1)
                readByte() == 0x01.toByte()
            }

            // value class Int
            IppTag.Integer,
            IppTag.Enum -> {
                assertValueLength(4)
                readInt()
            }

            // value class IppIntegerRange
            IppTag.RangeOfInteger -> {
                assertValueLength(8)
                IppIntegerRange(readInt(), readInt())
            }

            // value class IppResolution
            IppTag.Resolution -> {
                assertValueLength(9)
                IppResolution(readInt(), readInt(), readByte().toInt())
            }

            // value class String with rfc 8011 3.9 and rfc 8011 4.1.4.1 attribute value encoding
            IppTag.Uri -> URI.create(readString(charsetForTag(tag)))
            IppTag.OctetString,
            IppTag.Keyword,
            IppTag.UriScheme,
            IppTag.Charset,
            IppTag.NaturalLanguage,
            IppTag.MimeMediaType -> readString(charsetForTag(tag))

            // value class IppString
            IppTag.TextWithoutLanguage,
            IppTag.NameWithoutLanguage -> IppString(string = readString(charsetForTag(tag)))

            IppTag.TextWithLanguage,
            IppTag.NameWithLanguage -> {
                dataInputStream.readShort() // ignore redundant value length
                IppString(language = readString(charsetForTag(tag)), string = readString(charsetForTag(tag)))
            }

            // value class IppDateTime
            IppTag.DateTime -> {
                assertValueLength(11)
                IppDateTime(
                        year = readShort().toInt(),
                        month = read(),
                        day = read(),
                        hour = read(),
                        minutes = read(),
                        seconds = read(),
                        deciSeconds = read(),
                        directionFromUTC = readByte().toChar(),
                        hoursFromUTC = read(),
                        minutesFromUTC = read()
                )
            }

            else -> {
                readLengthAndValue()
                String.format("<$tag-decoding-not-implemented>")
            }
        }
    }

    private fun readString(charset: Charset): String {
        val bytes = readLengthAndValue()
        return String(bytes, charset)
    }

    private fun readLengthAndValue(): ByteArray {
        val length = dataInputStream.readShort().toInt()
        return dataInputStream.readNBytes(length)
    }

    private fun assertValueLength(expected: Int) {
        val length = dataInputStream.readShort().toInt()
        if (length != expected) {
            throw IppSpecViolation("expected value length of $expected bytes but found $length")
        }
    }

    override fun close() = dataInputStream.close()

}