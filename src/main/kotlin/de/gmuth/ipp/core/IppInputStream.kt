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
    }

    private val dataInputStream: DataInputStream = DataInputStream(inputStream)
    private var attributesCharset: Charset? = null // encoding for text and name attributes, rfc 8011 4.1.4.1
    var statusMessage: String? = null

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

    private fun readVersion() = with(dataInputStream) { IppVersion(readByte().toInt(), readByte().toInt()) }

    private fun readCode() = dataInputStream.readShort()

    private fun readRequestId() = dataInputStream.readInt()

    private fun readTag(): IppTag = IppTag.fromCode(dataInputStream.readByte())

    private fun readAttribute(tag: IppTag): IppAttribute<*> {
        val name = readString(Charsets.US_ASCII)
        var value = readAttributeValue(tag)

        // check tag
        if (compareTagsToIppRegistrations) IppRegistrations.checkSyntaxOfAttribute(name, tag)

        // collect special attribute values or convert types
        if (!tag.isOutOfBandTag()) when (name) {
            "attributes-charset" -> attributesCharset = Charset.forName(value as String)
            "status-message" -> statusMessage = value as String
            "job-state" -> value = IppJobState.fromCode(value as Int)
        }

        return IppAttribute(name, tag, value)
    }

    private fun readAttributeValue(tag: IppTag): Any? = when (tag) {

        // out-of-band
        IppTag.NoValue -> {
            assertValueLength(0)
            null
        }

        // value class Int
        IppTag.Integer,
        IppTag.Enum -> {
            assertValueLength(4)
            dataInputStream.readInt()
        }

        // value class String with rfc 8011 3.9 and rfc 8011 4.1.4.1 attribute value encoding
        IppTag.Uri -> URI.create(readString(charsetForTag(tag)))
        IppTag.Keyword,
        IppTag.UriScheme,
        IppTag.Charset,
        IppTag.NaturalLanguage,
        IppTag.MimeMediaType,
        IppTag.TextWithoutLanguage,
        IppTag.NameWithoutLanguage -> readString(charsetForTag(tag))

        else -> {
            // if support for a specific tag is required kindly ask the author to implement it
            readLengthAndValue()
            String.format("<$tag-decoding-not-implemented>")
        }
    }

    private fun charsetForTag(tag: IppTag) =
            if (tag.useAttributesCharset()) attributesCharset ?: throw IllegalStateException("missing attributes-charset")
            else Charsets.US_ASCII

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