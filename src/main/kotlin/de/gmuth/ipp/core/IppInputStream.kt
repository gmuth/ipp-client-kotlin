package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.Closeable
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset

class IppInputStream(private val inputStream: InputStream) : Closeable by inputStream {

    private val dataInputStream: DataInputStream = DataInputStream(inputStream)
    private var currentGroupTag: IppTag? = null
    private var currentAttributeTag: IppTag? = null

    var charset = Charsets.US_ASCII
    var statusMessage: String? = null

    override fun close() {
        dataInputStream.close()
        inputStream.close()
    }

    fun readVersion() = with(dataInputStream) { IppVersion(readByte().toInt(), readByte().toInt()) }

    fun readCode() = dataInputStream.readShort()

    fun readRequestId() = dataInputStream.readInt()

    fun readTag(): IppTag {
        val tag = IppTag.fromByte(dataInputStream.readByte())
        if (tag.isGroupTag())
            currentGroupTag = tag
        else
            currentAttributeTag = tag
        return tag
    }

    fun readAttribute(tag: IppTag): Pair<String, Any> {
        val name = String(readLengthAndValue(), charset)
        var value: Any = when (tag) {

            // value class Int
            IppTag.Integer,
            IppTag.Enum -> {
                assertValueLength(4)
                dataInputStream.readInt()
            }

            // value class String
            IppTag.TextWithoutLanguage,
            IppTag.Keyword,
            IppTag.Uri,
            IppTag.Charset,
            IppTag.NaturalLanguage,
            IppTag.MimeMediaType -> {
                String(readLengthAndValue(), charset)
            }

            else -> {
                // if support for a specific tag is required kindly ask the author to implement it
                readLengthAndValue()
                String.format("<decoding-tag-$tag(%02X)-not-implemented>", tag.value)
            }
        }
        // collect special attribute values
        when (name) {
            "attributes-charset" -> charset = Charset.forName(value as String)
            "status-message" -> statusMessage = value as String
            "job-state" -> value = IppJobState.fromInt(value as Int)
        }
        return Pair(name, value)
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

}