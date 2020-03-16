package de.gmuth.ipp

/**
 * Author: Gerhard Muth
 */

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset

open class IppMessage(
        var version: IppVersion = IppVersion(),
        private var code: Short = 0,
        var requestId: Int = 1,
        // values for required operation attributes
        var charset: Charset = Charsets.UTF_8,
        var naturalLanguage: String = "en"
) {
    // request context
    var operation: IppOperation
        get() = IppOperation.fromShort(code)
        set(value) {
            code = value.code
        }

    // response context
    val status: IppStatus
        get() = IppStatus.fromShort(code)

    var statusMessage: String? = null

    // --------------------------------------------------------------------- IPP ENCODING

    fun writeTo(outputStream: OutputStream) {
        with(IppOutputStream(outputStream, charset)) {
            writeVersion(version)
            writeCode(code)
            writeRequestId(requestId)

            writeTag(IppTag.Operation)
            writeAttribute(IppTag.Charset, "attributes-charset", charset.name().toLowerCase())
            writeAttribute(IppTag.NaturalLanguage, "attributes-natural-language", naturalLanguage)
            writeOperationAttributes(this)

            writeJobGroups(this)
            writeTag(IppTag.End)
            close()
        }
    }

    open fun writeOperationAttributes(ippOutputStream: IppOutputStream) {
        // implement in subclass for extra attributes
    }

    open fun writeJobGroups(ippOutputStream: IppOutputStream) {
        // implement in subclass for extra attributes
    }

    fun toByteArray(): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        writeTo(byteArrayOutputStream)
        byteArrayOutputStream.close()
        return byteArrayOutputStream.toByteArray()
    }

    fun toInputStream(): InputStream = ByteArrayInputStream(toByteArray())

    // --------------------------------------------------------------------- IPP DECODING

    companion object {
        var verbose: Boolean = false
        fun ofInputStream(inputStream: InputStream) = IppMessage().apply { readFrom(inputStream) }
        fun ofByteArray(byteArray: ByteArray) = ofInputStream(ByteArrayInputStream(byteArray))
    }

    fun readFrom(inputStream: InputStream) {
        val message = this
        val ippInputStream = IppInputStream(inputStream)

        val version = ippInputStream.readVersion()
        if (verbose) println("version = $version")

        code = ippInputStream.readCode()
        if (verbose) println(String.format("code = %04X", code))

        requestId = ippInputStream.readRequestId()
        if (verbose) println("request id = $requestId")

        var tag: IppTag
        do {
            tag = ippInputStream.readTag()
            if (tag.isGroupTag()) {
                if (verbose) println(String.format("%s group", tag))

            } else {
                // attribute tags
                val (name, value) = ippInputStream.readAttribute(tag)
                if (verbose) println(String.format("  %s (%s) = %s", name, tag, value))
            }
        } while (tag != IppTag.End)
        ippInputStream.close()

        // for response messages copy from ippInputStream
        message.statusMessage = statusMessage
    }

}