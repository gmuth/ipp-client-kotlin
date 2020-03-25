package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.*
import java.nio.charset.Charset

class IppOutputStream(outputStream: OutputStream, val attributesCharset: Charset) : Closeable, Flushable {

    private val dataOutputStream: DataOutputStream = DataOutputStream(outputStream)

    fun writeVersion(version: IppVersion) = with(dataOutputStream) { writeByte(version.major); writeByte(version.minor) }

    fun writeCode(code: Short) = dataOutputStream.writeShort(code.toInt())

    fun writeRequestId(requestId: Int = 0) = dataOutputStream.writeInt(requestId)

    fun writeTag(tag: IppTag) = dataOutputStream.writeByte(tag.value.toInt())

    fun writeGroup(attributesGroup: IppAttributesGroup) {
        writeTag(attributesGroup.tag)
        for (attribute in attributesGroup.values) {
            writeAttribute(attribute)
        }
    }

    private fun writeAttribute(attribute: IppAttribute<*>) {
        with(attribute) {
            writeTag(tag)
            writeLengthAndValue(name.toByteArray(Charsets.US_ASCII))

            //println("*** write value $tag $value --- ${value?.javaClass}")
            when (tag) {

                // value class Int
                IppTag.Integer,
                IppTag.Enum -> {
                    dataOutputStream.writeShort(4)
                    dataOutputStream.writeInt(value as Int)
                }

                // value class String with rfc 8011 3.9 attribute value encoding
                IppTag.Keyword,
                IppTag.Uri,
                IppTag.UriScheme,
                IppTag.Charset,
                IppTag.NaturalLanguage,
                IppTag.MimeMediaType -> {
                    writeLengthAndValue((value as String).toByteArray(Charsets.US_ASCII))
                }
                // value class String with rfc 8011 4.1.4.1 attributes-charset encoding
                IppTag.TextWithoutLanguage,
                IppTag.NameWithoutLanguage -> {
                    writeLengthAndValue((value as String).toByteArray(attributesCharset))
                }

                else -> {
                    // if support for a specific tag is required kindly ask the author to implement it
                    throw IOException(String.format("tag %s (%02X) encoding not implemented", tag, tag.value))
                }
            }
        }
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