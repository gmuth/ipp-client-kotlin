package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.Closeable
import java.io.DataOutputStream
import java.io.Flushable
import java.io.OutputStream
import java.net.URI
import java.nio.charset.Charset

class IppOutputStream(outputStream: OutputStream, private val attributesCharset: Charset) : Closeable, Flushable {

    private val dataOutputStream: DataOutputStream = DataOutputStream(outputStream)

    fun writeVersion(version: IppVersion) = with(dataOutputStream) { writeByte(version.major); writeByte(version.minor) }

    fun writeCode(code: Short) = dataOutputStream.writeShort(code.toInt())

    fun writeRequestId(requestId: Int = 0) = dataOutputStream.writeInt(requestId)

    fun writeTag(tag: IppTag) = dataOutputStream.writeByte(tag.code.toInt())

    fun writeAttributesGroup(attributesGroup: IppAttributesGroup) {
        writeTag(attributesGroup.tag)
        attributesGroup.values.forEach { attribute -> writeAttribute(attribute) }
    }

    private fun writeAttribute(attribute: IppAttribute<*>) {
        val tag = attribute.tag
        writeTag(tag)
        writeString(attribute.name, Charsets.US_ASCII)

        val value = attribute.value
        //println("*** write value $tag $value --- ${value?.javaClass}")
        when (tag) {

            // value class Int
            IppTag.Integer,
            IppTag.Enum -> {
                dataOutputStream.writeShort(4)
                dataOutputStream.writeInt(value as Int)
            }

            // value class String with rfc 8011 3.9 and rfc 8011 4.1.4.1 attribute value encoding
            IppTag.Uri -> writeString((value as URI).toString(), charsetForIppTag(tag))
            IppTag.Keyword,
            IppTag.UriScheme,
            IppTag.Charset,
            IppTag.NaturalLanguage,
            IppTag.MimeMediaType,
            IppTag.TextWithoutLanguage,
            IppTag.NameWithoutLanguage -> writeString(value as String, charsetForIppTag(tag))

            else -> throw NotImplementedError(String.format("tag %s (%02X) encoding not implemented", tag, tag.code))
        }
    }

    private fun charsetForIppTag(ippTag: IppTag) =
            if (ippTag.useAttributesCharsetEncoding()) attributesCharset
            else Charsets.US_ASCII

    private fun writeString(value: String, charset: Charset) {
        writeLengthAndValue(value.toByteArray(charset))
    }

    private fun writeLengthAndValue(value: ByteArray) {
        dataOutputStream.writeShort(value.size)
        dataOutputStream.write(value)
    }

    override fun close() {
        dataOutputStream.close()
    }

    override fun flush() {
        dataOutputStream.flush()
    }

}