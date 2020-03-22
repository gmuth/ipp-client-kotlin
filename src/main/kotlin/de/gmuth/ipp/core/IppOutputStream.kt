package de.gmuth.ipp.core

/**
 * Author: Gerhard Muth
 */

import java.io.*
import java.nio.charset.Charset

class IppOutputStream(private val outputStream: OutputStream, val charset: Charset = Charsets.US_ASCII) : Closeable, Flushable {

    private val dataOutputStream: DataOutputStream = DataOutputStream(outputStream)

    override fun close() {
        dataOutputStream.close()
        outputStream.close()
    }

    override fun flush() {
        dataOutputStream.flush()
        outputStream.flush()
    }

    fun writeVersion(version: IppVersion) = with(dataOutputStream) { writeByte(version.major); writeByte(version.minor) }

    fun writeCode(code: Short) = dataOutputStream.writeShort(code.toInt())

    fun writeRequestId(requestId: Int = 0) = dataOutputStream.writeInt(requestId)

    fun writeTag(tag: IppTag) = dataOutputStream.writeByte(tag.value.toInt())

    fun writeAttribute(tag: IppTag, name: String, value: Any) {
        writeTag(tag)
        writeLengthAndValue(name.toByteArray(charset))
        when (tag) {

            // value class Int
            IppTag.Integer,
            IppTag.Enum -> {
                dataOutputStream.writeShort(4)
                dataOutputStream.writeInt(value as Int)
            }

            // value class String
            IppTag.TextWithoutLanguage,
            IppTag.Uri,
            IppTag.Charset,
            IppTag.NaturalLanguage,
            IppTag.MimeMediaType -> {
                writeLengthAndValue((value as String).toByteArray(charset))
            }
            else -> {
                // if support for a specific tag is required kindly ask the author to implement it
                throw IOException(String.format("tag %s (%02X) encoding not implemented", tag, tag.value))
            }
        }
    }

    private fun writeLengthAndValue(value: ByteArray) {
        dataOutputStream.writeShort(value.size)
        dataOutputStream.write(value)
    }

}