package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.io.ByteArraySavingBufferedInputStream
import de.gmuth.io.ByteArraySavingOutputStream
import de.gmuth.log.Logging
import java.io.*

abstract class IppMessage {

    var code: Short? = null
    var requestId: Int? = null
    var version: String? = null
    val attributesGroups = mutableListOf<IppAttributesGroup>()
    var documentInputStream: InputStream? = null
    var documentInputStreamIsConsumed: Boolean = false
    var rawBytes: ByteArray? = null

    abstract val codeDescription: String // request operation or response status

    companion object {
        val log = Logging.getLogger {}
    }

    val operationGroup: IppAttributesGroup
        get() = getSingleAttributesGroup(IppTag.Operation)

    val jobGroup: IppAttributesGroup
        get() = getSingleAttributesGroup(IppTag.Job)

    fun getAttributesGroups(tag: IppTag) =
            attributesGroups.filter { it.tag == tag }

    fun getSingleAttributesGroup(tag: IppTag) = with(getAttributesGroups(tag)) {
        if (isEmpty()) throw IppException("no group found with tag '$tag' in $attributesGroups")
        single()
    }

    fun containsGroup(tag: IppTag) =
            attributesGroups.map { it.tag }.contains(tag)

    // factory method for IppAttributesGroup
    fun createAttributesGroup(tag: IppTag) =
            IppAttributesGroup(tag).apply { attributesGroups.add(this) }

    // --------
    // ENCODING
    // --------

    fun write(outputStream: OutputStream) {
        val byteArraySavingOutputStream = ByteArraySavingOutputStream(outputStream)
        try {
            IppOutputStream(byteArraySavingOutputStream).writeMessage(this)
        } finally {
            rawBytes = byteArraySavingOutputStream.toByteArray()
        }
        copyDocumentStream(outputStream)
    }

    fun write(file: File) =
            write(FileOutputStream(file))

    fun encode(): ByteArray = with(ByteArrayOutputStream()) {
        write(this)
        toByteArray()
    }

    // --------
    // DECODING
    // --------

    fun read(inputStream: InputStream) {
        val byteArraySavingBufferedInputStream = ByteArraySavingBufferedInputStream(inputStream)
        try {
            IppInputStream(byteArraySavingBufferedInputStream).readMessage(this)
            documentInputStream = byteArraySavingBufferedInputStream
        } finally {
            rawBytes = byteArraySavingBufferedInputStream.toByteArray()
            byteArraySavingBufferedInputStream.duplicateBytes = false // stop saving bytes (document)
        }
    }

    fun read(file: File) {
        log.debug { "read file ${file.absolutePath} (${file.length()} bytes)" }
        read(FileInputStream(file))
    }

    fun decode(byteArray: ByteArray) {
        log.debug { "decode ${byteArray.size} bytes" }
        read(ByteArrayInputStream(byteArray))
    }

    // ------------------------
    // DOCUMENT and IPP-MESSAGE
    // ------------------------

    private fun copyDocumentStream(outputStream: OutputStream) {
        if (documentInputStreamIsConsumed) log.warn { "documentInputStream is consumed" }
        documentInputStream?.copyTo(outputStream)
        log.debug { "consumed documentInputStream" }
        documentInputStreamIsConsumed = true
    }

    fun saveDocumentStream(file: File) {
        copyDocumentStream(file.outputStream())
        log.info { "saved ${file.length()} document bytes to file ${file.path}" }
    }

    fun saveRawBytes(file: File) = file.apply {
        writeBytes(rawBytes ?: throw RuntimeException("missing raw bytes. you must call read/decode or write/encode before."))
    }

    // -------
    // LOGGING
    // -------

    override fun toString() = "%s %s%s".format(
            codeDescription,
            attributesGroups.map { "${it.values.size} ${it.tag}" },
            if (rawBytes == null) "" else " (${rawBytes!!.size} bytes)"
    )

    fun logDetails(prefix: String = "") {
        if (rawBytes != null) log.info { "${prefix}${rawBytes!!.size} raw ipp bytes" }
        log.info { "${prefix}version = $version" }
        log.info { "${prefix}$codeDescription" }
        log.info { "${prefix}request-id = $requestId" }
        for (group in attributesGroups) {
            group.logDetails(prefix)
        }
    }

}