package de.gmuth.ipp.core

/**
 * Author: Gerhard Muth
 */

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset

abstract class IppMessage {

    var version: IppVersion = IppVersion()
    protected var code: Short? = null
    var requestId: Int? = null

    // --------------------------------------------------------------------- IPP MESSAGE ENCODING

    fun writeTo(outputStream: OutputStream, charset: Charset, naturalLanguage: String) {
        if (code == null) throw IllegalArgumentException("code must not be null!")
        if (requestId == null) throw IllegalArgumentException("requestId must not be null!")

        with(IppOutputStream(outputStream, charset)) {
            writeVersion(version)
            writeCode(code as Short)
            writeRequestId(requestId as Int)

            writeTag(IppTag.Operation)
            writeAttribute(IppTag.Charset, "attributes-charset", this.charset.name().toLowerCase())
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

    fun toByteArray(charset: Charset, naturalLanguage: String): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        writeTo(byteArrayOutputStream, charset, naturalLanguage)
        byteArrayOutputStream.close()
        return byteArrayOutputStream.toByteArray()
    }

    fun toInputStream(charset: Charset, naturalLanguage: String): InputStream = ByteArrayInputStream(toByteArray(charset, naturalLanguage))

    // --------------------------------------------------------------------- IPP MESSAGE DECODING

    companion object {
        var verbose: Boolean = false
    }

    open fun readFrom(inputStream: InputStream): String? {
        val ippInputStream = IppInputStream(inputStream)

        val version = ippInputStream.readVersion()
        if (verbose) println("version = $version")

        code = ippInputStream.readCode()
        if (verbose) println(getCodeDescription())

        requestId = ippInputStream.readRequestId()
        if (verbose) println("request-id = $requestId")

        var tag: IppTag
        do {
            tag = ippInputStream.readTag()
            if (tag.isGroupTag()) {
                if (tag != IppTag.End && verbose)
                    println(String.format("%s group", tag))

            } else {
                // attribute tags
                val (name, value) = ippInputStream.readAttribute(tag)
                if (verbose) println(String.format("  %s (%s) = %s", name, tag, value))
            }
        } while (tag != IppTag.End)
        ippInputStream.close()
        //this.charset = ippInputStream.charset
        return ippInputStream.statusMessage;
    }

    open fun getCodeDescription(): String = String.format("code = %04X", code)

}