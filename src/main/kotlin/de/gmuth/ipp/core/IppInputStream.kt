package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.Closeable
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.nio.charset.Charset

class IppInputStream(inputStream: InputStream) : Closeable by inputStream {

    private val dataInputStream: DataInputStream = DataInputStream(inputStream)

    private var attributesCharset: Charset? = null // encoding for text and name attributes, rfc 8011 4.1.4.1
    var statusMessage: String? = null

    fun readVersion() = with(dataInputStream) { IppVersion(readByte().toInt(), readByte().toInt()) }

    fun readCode() = dataInputStream.readShort()

    fun readRequestId() = dataInputStream.readInt()

    fun readTag(): IppTag = IppTag.fromByte(dataInputStream.readByte())

    fun readAttribute(tag: IppTag): IppAttribute<*> {
        val name = String(readLengthAndValue(), Charsets.US_ASCII)
        var value: Any = when (tag) {

            // value class Int
            IppTag.Integer,
            IppTag.Enum -> {
                assertValueLength(4)
                dataInputStream.readInt()
            }

            // value class String with rfc 8011 3.9 attribute value encoding
            IppTag.Keyword,
            IppTag.Uri,
            IppTag.UriScheme,
            IppTag.Charset,
            IppTag.NaturalLanguage,
            IppTag.MimeMediaType -> String(readLengthAndValue(), Charsets.US_ASCII)

            // value class String with rfc 8011 4.1.4.1 attributes-charset encoding
            IppTag.TextWithoutLanguage,
            IppTag.NameWithoutLanguage -> {
                String(readLengthAndValue(), attributesCharset ?: throw IllegalStateException("missing attributes-charset"))
            }

            else -> {
                // if support for a specific tag is required kindly ask the author to implement it
                readLengthAndValue()
                String.format("<$tag-decoding-not-implemented>")
            }
        }

        // convert some types
        when(tag) {
            IppTag.Uri -> value = URI.create(value as String)
        }

        // collect special attribute values
        when (name) {
            "attributes-charset" -> attributesCharset = Charset.forName(value as String)
            "status-message" -> statusMessage = value as String
            "job-state" -> value = IppJobState.fromInt(value as Int) // type conversion
        }

        return IppAttribute(name, tag, value)
    }

    private fun readLengthAndValue(): ByteArray {
        val length = dataInputStream.readShort().toInt()
        // setOf not yet supported :-(
        if (length == 0) println("warn: found ipp value with 0 bytes")
        return dataInputStream.readNBytes(length)
    }

    private fun assertValueLength(expected: Int) {
        val length = dataInputStream.readShort().toInt()
        if (length != expected) {
            throw IOException("expected value length of $expected bytes but found $length")
        }
    }

    override fun close() {
        dataInputStream.close()
    }

}