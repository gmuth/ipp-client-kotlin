package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2021 Gerhard Muth
 */

import de.gmuth.io.ByteArraySavingInputStream
import de.gmuth.io.ByteArraySavingOutputStream
import de.gmuth.ipp.core.IppTag.*
import de.gmuth.log.Logging
import java.io.*

abstract class IppMessage() {

    var code: Short? = null
    var requestId: Int? = null
    var version: String? = null
        set(value) { // validate version
            if (Regex("""^\d\.\d$""").matches(value!!)) field = value else throw IppException("invalid version string: $value")
        }
    val attributesGroups = mutableListOf<IppAttributesGroup>()
    var documentInputStream: InputStream? = null
    var documentInputStreamIsConsumed: Boolean = false
    var rawBytes: ByteArray? = null

    abstract val codeDescription: String // request operation or response status

    companion object {
        val log = Logging.getLogger {}
    }

    constructor(version: String, requestId: Int, charset: java.nio.charset.Charset, naturalLanguage: String) : this() {
        this.version = version
        this.requestId = requestId
        createAttributesGroup(Operation).run {
            attribute("attributes-charset", Charset, charset)
            attribute("attributes-natural-language", NaturalLanguage, naturalLanguage)
        }
    }

    val operationGroup: IppAttributesGroup
        get() = getSingleAttributesGroup(Operation)

    val jobGroup: IppAttributesGroup
        get() = getSingleAttributesGroup(Job)

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

    fun hasDocument() = documentInputStream != null

    // --------
    // ENCODING
    // --------

    fun write(outputStream: OutputStream) {
        val byteArraySavingOutputStream = ByteArraySavingOutputStream(outputStream)
        try {
            IppOutputStream(byteArraySavingOutputStream).writeMessage(this)
        } finally {
            rawBytes = byteArraySavingOutputStream.toByteArray()
            log.debug { "wrote raw ipp message: ${rawBytes!!.size} bytes" }
            byteArraySavingOutputStream.saveBytes = false // stop saving document bytes
        }
        copyDocumentStream(byteArraySavingOutputStream)
    }

    fun write(file: File) = write(FileOutputStream(file))

    fun encode(): ByteArray = with(ByteArrayOutputStream()) {
        write(this)
        log.debug { "ByteArrayOutputStream size: ${this.size()} bytes" }
        toByteArray()
    }

    fun encodeAsInputStream() = ByteArrayInputStream(encode())

    // --------
    // DECODING
    // --------

    fun read(inputStream: InputStream) {
        val byteArraySavingInputStream = ByteArraySavingInputStream(inputStream)
        val bufferedInputStream = byteArraySavingInputStream.buffered()
        try {
            IppInputStream(bufferedInputStream).readMessage(this)
            documentInputStream = bufferedInputStream
        } finally {
            rawBytes = byteArraySavingInputStream.toByteArray()
            log.debug { "read ${rawBytes!!.size} raw bytes" }
        }
    }

    fun read(file: File) {
        log.debug { "read file ${file.absolutePath}: ${file.length()} bytes" }
        read(FileInputStream(file))
    }

    fun decode(byteArray: ByteArray) {
        log.debug { "decode ${byteArray.size} bytes" }
        read(ByteArrayInputStream(byteArray))
    }

    // ------------------------
    // DOCUMENT and IPP-MESSAGE
    // ------------------------

    private fun copyDocumentStream(outputStream: OutputStream): Long? {
        if (documentInputStreamIsConsumed) log.warn { "documentInputStream is consumed" }
        return documentInputStream?.copyTo(outputStream).apply {
            log.debug { "consumed documentInputStream: $this bytes" }
            documentInputStreamIsConsumed = true
        }
    }

    fun saveDocumentStream(file: File) {
        copyDocumentStream(file.outputStream())
        log.info { "saved ${file.length()} document bytes to file ${file.path}" }
    }

    fun saveRawBytes(file: File) =
        if (rawBytes == null) {
            log.warn { "no raw bytes to save. you must call read/decode or write/encode before." }
        } else {
            file.writeBytes(rawBytes!!)
            log.info { "saved ${file.path} (${file.length()} bytes)" }
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