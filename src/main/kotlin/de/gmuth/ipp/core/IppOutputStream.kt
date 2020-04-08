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

class IppOutputStream(outputStream: OutputStream) : Closeable, Flushable {

    private val dataOutputStream: DataOutputStream = DataOutputStream(outputStream)

    private var attributesCharset: Charset? = null // encoding for text and name attributes, rfc 8011 4.1.4.1

    private fun charsetForTag(tag: IppTag) =
            if (tag.useAttributesCharset()) attributesCharset ?: throw IppException("missing attributes-charset")
            else Charsets.US_ASCII

    fun writeMessage(message: IppMessage) {
        with(message) {
            writeVersion(version ?: throw IppException("missing version"))
            writeCode(code ?: throw IppException("missing operation or status code"))
            writeRequestId(requestId ?: throw IppException("missing requestIds"))
            attributesGroups.forEach { group ->
                writeAttributesGroup(group)
            }
            writeTag(IppTag.End)
        }
    }

    private fun writeVersion(version: IppVersion) = with(dataOutputStream) { writeByte(version.major); writeByte(version.minor) }

    private fun writeCode(code: Short) = dataOutputStream.writeShort(code.toInt())

    private fun writeRequestId(requestId: Int = 0) = dataOutputStream.writeInt(requestId)

    private fun writeAttributesGroup(attributesGroup: IppAttributesGroup) {
        with(attributesGroup) {
            if (size > 0) {
                writeTag(tag)
                for (attribute in values) {
                    writeAttribute(attribute)
                }
            }
        }
    }

    private fun writeAttribute(attribute: IppAttribute<*>) {
        with(attribute) {
            IppRegistrations.checkSyntaxOfAttribute(name, tag)
            if (tag != IppTag.NoValue && values.isEmpty()) {
                throw IppException("no values found to write for '$name'")
            }
            // 1setOf iteration
            for ((index, value) in values.withIndex()) {
                //println("*** $name[$index] = ($tag) $value")
                writeTag(tag)
                writeString(if (index == 0) name else "", Charsets.US_ASCII)
                writeAttributeValue(tag, value)
            }
            if (name == "attributes-charset" && tag == IppTag.Charset) {
                attributesCharset = Charset.forName(value as String)
            }
        }
    }

    private fun writeTag(tag: IppTag) = dataOutputStream.writeByte(tag.code.toInt())

    private fun writeAttributeValue(tag: IppTag, value: Any?) {
        if (value == null && tag != IppTag.NoValue) {
            println("WARN: value is null")
        }
        //tag.validateValueClass(value)
        when (tag) {
            // out-of-band
            IppTag.NoValue -> {
                dataOutputStream.writeShort(0)
            }

            // value class Int
            IppTag.Integer,
            IppTag.Enum -> {
                dataOutputStream.writeShort(4)
                dataOutputStream.writeInt(value as Int)
            }

            // value class String with rfc 8011 3.9 and rfc 8011 4.1.4.1 attribute value encoding
            IppTag.Uri -> writeString((value as URI).toString(), charsetForTag(tag))
            IppTag.Keyword,
            IppTag.UriScheme,
            IppTag.Charset,
            IppTag.NaturalLanguage,
            IppTag.MimeMediaType,
            IppTag.TextWithoutLanguage,
            IppTag.NameWithoutLanguage -> writeString(value as String, charsetForTag(tag))

            else -> throw IppException(String.format("tag %s (%02X) encoding not implemented", tag, tag.code))
        }
    }

    private fun writeString(value: String, charset: Charset) {
        writeLengthAndValue(value.toByteArray(charset))
    }

    private fun writeLengthAndValue(value: ByteArray) {
        dataOutputStream.writeShort(value.size)
        dataOutputStream.write(value)
    }

    override fun close() = dataOutputStream.close()

    override fun flush() = dataOutputStream.flush()

}