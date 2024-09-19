package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2024 Gerhard Muth
 */

import de.gmuth.ipp.core.IppTag.*
import java.io.*
import java.util.logging.Level
import java.util.logging.Level.INFO
import java.util.logging.Logger
import java.util.logging.Logger.getLogger
import java.nio.charset.Charset as javaCharset

abstract class IppMessage() {

    companion object {
        var keepDocumentCopy: Boolean = false
    }

    private val logger = getLogger(javaClass.name)
    var code: Int? = null // unsigned short (16 bits)
    var requestId: Int? = null
    var version: String? = null
        set(value) { // validate version
            if (Regex("""^\d\.\d$""").matches(value!!)) field = value
            else throw IppException("Invalid version string: $value")
        }
    val attributesGroups = mutableListOf<IppAttributesGroup>()
    var documentInputStream: InputStream? = null
    var documentInputStreamIsConsumed: Boolean = false
    var rawBytes: ByteArray? = null
    var documentBytes: ByteArray? = null

    abstract val codeDescription: String // request operation or response status

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

    val printerGroup: IppAttributesGroup // also used for CUPS-Printer-Operation requests
        get() = getSingleAttributesGroup(Printer)

    val jobGroup: IppAttributesGroup
        get() = getSingleAttributesGroup(Job)

    val subscriptionGroup: IppAttributesGroup
        get() = getSingleAttributesGroup(Subscription)

    fun getAttributesGroups(tag: IppTag) =
        attributesGroups.filter { it.tag == tag }

    fun getSingleAttributesGroup(tag: IppTag) = getAttributesGroups(tag).run {
        if (isEmpty()) throw IppException("No group found with tag '$tag' in $attributesGroups")
        single()
    }

    fun containsGroup(tag: IppTag) =
        attributesGroups.map { it.tag }.contains(tag)

    // factory method for IppAttributesGroup
    fun createAttributesGroup(tag: IppTag) =
        IppAttributesGroup(tag).apply { attributesGroups.add(this) }

    fun hasDocument() = documentInputStream != null

    val attributesCharset: javaCharset
        get() = operationGroup.getValue("attributes-charset")

    val naturalLanguage: String
        get() = operationGroup.getValue("attributes-natural-language")

    val requestingUserName: String
        get() = operationGroup.getValueAsString("requesting-user-name")

    // --------
    // ENCODING
    // --------

    fun write(outputStream: OutputStream) {
        val byteArraySavingOutputStream = ByteArraySavingOutputStream(outputStream)
        try {
            IppOutputStream(byteArraySavingOutputStream).writeMessage(this)
        } finally {
            rawBytes = byteArraySavingOutputStream.getSavedBytes()
        }
        if (hasDocument()) copyDocumentStream(outputStream)
    }

    fun write(file: File) =
        write(FileOutputStream(file))

    fun encode(): ByteArray = ByteArrayOutputStream().run {
        write(this)
        toByteArray()
    }

    // --------
    // DECODING
    // --------

    fun read(inputStream: InputStream) {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val byteArraySavingInputStream = object : InputStream() {
            override fun read() = inputStream.read()
                .also { if (it != -1) byteArrayOutputStream.write(it) }
        }
        val bufferedInputStream = byteArraySavingInputStream.buffered()
        try {
            IppInputStream(bufferedInputStream).readMessage(this)
            documentInputStream = bufferedInputStream
        } finally {
            rawBytes = byteArrayOutputStream.toByteArray()
        }
    }

    fun read(file: File) {
        logger.finer { "Read file ${file.absolutePath}: ${file.length()} bytes" }
        read(FileInputStream(file))
    }

    fun decode(byteArray: ByteArray) {
        logger.finer { "Decode ${byteArray.size} bytes" }
        read(ByteArrayInputStream(byteArray))
    }

    // ------------------------
    // DOCUMENT and IPP-MESSAGE
    // ------------------------

    protected fun copyDocumentStream(outputStream: OutputStream): Long {
        if (documentInputStreamIsConsumed) logger.warning { "documentInputStream is consumed" }
        val byteArraySavingOutputStream = ByteArraySavingOutputStream(outputStream)
        return documentInputStream!!
            .copyTo(if (keepDocumentCopy) byteArraySavingOutputStream else outputStream) // number of bytes copied
            .apply {
                logger.finer { "Consumed documentInputStream: $this bytes" }
                documentInputStreamIsConsumed = true
                if (keepDocumentCopy) {
                    documentBytes = byteArraySavingOutputStream.getSavedBytes()
                    if (documentBytes!!.isNotEmpty()) logger.info("Keeping ${documentBytes!!.size} document bytes")
                }
                byteArraySavingOutputStream.close()
            }
    }

    fun saveDocumentStream(file: File) {
        copyDocumentStream(file.outputStream())
        logger.fine { "Saved ${file.length()} document bytes to file ${file.path}" }
    }

    fun saveBytes(file: File) =
        if (rawBytes == null) {
            throw IppException("No raw bytes to save. You must call read/decode or write/encode before.")
        } else {
            file.writeBytes(rawBytes!!)
            logger.info { "Saved ${file.path} (${file.length()} bytes)" }
        }

    fun write(bufferedWriter: BufferedWriter, title: String? = null) {
        fun writeln(text: String) = bufferedWriter.run { write(text); newLine() }
        title?.also { bufferedWriter.write(it) }
        if (rawBytes != null) bufferedWriter.write(" (decoded ${rawBytes!!.size} raw IPP bytes)")
        bufferedWriter.newLine()
        writeln("version $version")
        writeln(codeDescription)
        writeln("request-id $requestId")
        for (group in attributesGroups) {
            group.write(bufferedWriter)
        }
    }

    fun saveText(file: File) = file.apply {
        bufferedWriter().use { write(it, title = "# File: ${file.name}") }
        logger.info { "Saved $path (${length()} bytes)" }
    }

    fun readBytesAndSaveText(
        bytesFile: File,
        textFile: File = with(bytesFile) { File(parentFile, "$name.txt") }
    ) = textFile.apply {
        read(bytesFile)
        saveText(textFile)
    }

    // -------
    // LOGGING
    // -------

    override fun toString() = "%s %s%s".format(
        codeDescription,
        attributesGroups.map { "${it.values.size} ${it.tag}" },
        if (rawBytes == null) "" else " (${rawBytes!!.size} bytes)"
    )

    @JvmOverloads
    open fun log(logger: Logger, level: Level = INFO, prefix: String = "") {
        if (rawBytes != null) logger.log(level) { "${prefix}${rawBytes!!.size} raw ipp bytes" }
        logger.log(level) { "${prefix}version = $version" }
        logger.log(level) { "${prefix}$codeDescription" }
        logger.log(level) { "${prefix}request-id = $requestId" }
        for (group in attributesGroups) {
            group.log(logger, level, prefix = prefix)
        }
    }

    private class ByteArraySavingOutputStream(private val outputStream: OutputStream) : OutputStream() {
        val byteArrayOutputStream = ByteArrayOutputStream()
        fun getSavedBytes(): ByteArray = byteArrayOutputStream.toByteArray()
        override fun write(byte: Int) = outputStream.write(byte)
            .also { byteArrayOutputStream.write(byte) }
    }
}