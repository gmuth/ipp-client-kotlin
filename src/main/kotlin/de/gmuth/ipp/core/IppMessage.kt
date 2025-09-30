package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2025 Gerhard Muth
 */

import de.gmuth.ipp.attributes.Compression
import de.gmuth.ipp.core.IppTag.*
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.logging.Level
import java.util.logging.Level.INFO
import java.util.logging.Logger
import java.util.logging.Logger.getLogger
import java.nio.charset.Charset
import kotlin.io.path.createDirectories
import kotlin.io.path.name

abstract class IppMessage() {

    companion object {
        var keepDocumentCopy: Boolean = false
    }

    private val logger = getLogger(IppMessage::class.java.name)
    var code: Int? = null // unsigned short (16 bits)
    var requestId: Int? = null
    var version: String? = null
        set(value) { // validate version
            if (Regex("""^\d\.\d$""").matches(value!!)) field = value
            else throw IppException("Invalid version string: $value")
        }
    val attributesGroups = mutableListOf<IppAttributesGroup>()
    var documentInputStream: InputStream? = null
    var rawBytes: ByteArray? = null
    var documentBytes: ByteArray? = null

    abstract val codeDescription: String // request operation or response status

    constructor(version: String, requestId: Int, charset: Charset, naturalLanguage: String) : this() {
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

    @Suppress("UNCHECKED_CAST")
    fun <T> getAttributeValuesOrNull(groupTag: IppTag, attributeName: String) =
        if (containsGroup(groupTag)) getSingleAttributesGroup(groupTag).getValuesOrNull(attributeName) as T?
        else null

    // factory method for IppAttributesGroup
    fun createAttributesGroup(tag: IppTag) =
        IppAttributesGroup(tag).apply { attributesGroups.add(this) }

    fun hasDocument() = documentInputStream != null

    val attributesCharset: Charset
        get() = operationGroup.getValue("attributes-charset")

    val naturalLanguage: String
        get() = operationGroup.getValue("attributes-natural-language")

    val compression: Compression
        get() = Compression.fromString(operationGroup.getValue("compression"))

    // --------
    // ENCODING
    // --------

    @JvmOverloads
    fun write(outputStream: OutputStream, writeDocumentIfAvailable: Boolean = true) {
        val byteArraySavingOutputStream = ByteArraySavingOutputStream(outputStream)
        try {
            IppOutputStream(byteArraySavingOutputStream).writeMessage(this)
        } finally {
            rawBytes = byteArraySavingOutputStream.getSavedBytes()
        }
        if (writeDocumentIfAvailable && hasDocument()) {
            val outputStreamWithCompressionSupport =
                if (operationGroup.containsKey("compression")) compression.getCompressingOutputStream(outputStream)
                else outputStream
            logger.fine { "Write document using ${outputStreamWithCompressionSupport.javaClass.simpleName}" }
            copyUnconsumedDocumentInputStream(outputStreamWithCompressionSupport)
            outputStreamWithCompressionSupport.close() // finalize compression
        }
    }

    fun write(file: File, writeDocumentIfAvailable: Boolean = false) =
        write(FileOutputStream(file), writeDocumentIfAvailable)

    fun write(path: Path, writeDocumentIfAvailable: Boolean = false) =
        write(Files.newOutputStream(path), writeDocumentIfAvailable)

    @JvmOverloads
    fun encode(appendDocumentIfAvailable: Boolean = true) = ByteArrayOutputStream().use {
        write(it, appendDocumentIfAvailable)
        it.toByteArray()
    }

    // --------
    // DECODING
    // --------

    fun read(inputStream: InputStream) {
        val byteArraySavingInputStream = ByteArraySavingInputStream(inputStream)
        val bufferedInputStream = byteArraySavingInputStream.buffered()
        try {
            IppInputStream(bufferedInputStream).readMessage(this)
            if (bufferedInputStream.available() == 0) {
                logger.finest { "No document bytes available from bufferedInputStream after readMessage()" }
            } else {
                documentInputStream =
                    if (operationGroup.containsKey("compression")) {
                        compression.getDecompressingInputStream(bufferedInputStream)
                    } else {
                        bufferedInputStream
                    }
                logger.fine { "documentInputStream class: ${documentInputStream!!.javaClass.simpleName}" }
            }
        } finally {
            // radar: could this include potentially buffered document bytes?
            rawBytes = byteArraySavingInputStream.getSavedBytes()
        }
    }

    fun read(file: File) {
        logger.finer { "Read file ${file.absolutePath}: ${file.length()} bytes" }
        read(FileInputStream(file))
    }

    fun read(path: Path) {
        logger.finer { "Read path ${path.toAbsolutePath()}: ${Files.size(path)} bytes" }
        read(Files.newInputStream(path))
    }

    fun decode(byteArray: ByteArray) {
        logger.finer { "Decode ${byteArray.size} bytes" }
        read(ByteArrayInputStream(byteArray))
    }

    // ------------------------
    // DOCUMENT and IPP-MESSAGE
    // ------------------------

    private fun copyUnconsumedDocumentInputStream(outputStream: OutputStream) {
        if (hasDocument() && documentInputStream!!.available() == 0) throw IppException("documentInputStream is consumed")
        val outputStreamWithCopySupport =
            if (keepDocumentCopy) ByteArraySavingOutputStream(outputStream)
            else outputStream
        documentInputStream!!
            .copyTo(outputStreamWithCopySupport) // returns number of bytes copied
            .apply { logger.finer { "Consumed documentInputStreamWithUncompressingSupport: $this bytes" } }
        if (outputStreamWithCopySupport is ByteArraySavingOutputStream) {
            documentBytes = outputStreamWithCopySupport.getSavedBytes()
            logger.finer("Keeping ${documentBytes!!.size} document bytes")
        }
    }

    fun writeDocument(outputStream: OutputStream) {
        if (documentInputStream!!.available() == 0) { // documentInputStreamConsumed
            if (documentBytes == null || documentBytes!!.isEmpty()) throw IppException(
                "Nothing available from documentInputStream. Enable IppMessage.keepDocumentCopy in order to keep documentBytes."
            )
            else outputStream.use { it.write(documentBytes!!) }
        } else copyUnconsumedDocumentInputStream(outputStream)
    }

    fun saveDocument(file: File) = file.run {
        writeDocument(outputStream())
        logger.info { "Saved ${length()} document bytes to $path" }
    }

    fun saveDocument(path: Path) {
        writeDocument(Files.newOutputStream(path))
        logger.info { "Saved ${Files.size(path)} document bytes to $path" }
    }

    fun writeBytes(outputStream: OutputStream) =
        if (rawBytes == null) throw IppException("No raw bytes to write. You must call read/decode or write/encode before.")
        else outputStream.write(rawBytes!!)

    fun saveBytes(file: File) = file.run {
        outputStream().use { writeBytes(it) }
        logger.info { "Saved $path (${length()} bytes)" }
    }

    fun saveBytes(path: Path) {
        path.parent?.createDirectories()
        Files.newOutputStream(path).use { writeBytes(it) }
        logger.info { "Saved $path (${Files.size(path)} bytes)" }
    }

    fun writeText(printWriter: PrintWriter, title: String? = null) = printWriter.apply {
        title?.let { println(it) }
        print("---------------------------------------------------------------------")
        rawBytes?.run { print(" $size raw bytes ----------") }
        println()
        println("version $version")
        println(codeDescription)
        println("request-id $requestId")
        attributesGroups.forEach { it.writeText(this) }
    }

    fun saveText(file: File) = file.apply {
        printWriter().use { writeText(it, title = "File: $name") }
        logger.info { "Saved $path (${length()} bytes)" }
    }

    fun saveText(path: Path) {
        path.parent?.createDirectories()
        Files.newBufferedWriter(path).use { writeText(PrintWriter(it), title = "File: ${path.fileName}") }
        logger.info { "Saved $path (${Files.size(path)} bytes)" }
    }

    fun readBytesAndSaveText(
        bytesFile: File,
        textFile: File = with(bytesFile) { File(parentFile, "$name.txt") }
    ) {
        read(bytesFile)
        saveText(textFile)
    }

    fun readBytesAndSaveText(
        bytesPath: Path,
        textPath: Path = bytesPath.parent.resolve("${bytesPath.name}.txt")
    ) {
        read(bytesPath)
        saveText(textPath)
    }

    // -------
    // LOGGING
    // -------

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

    // --- ByteArraySavingStreams keep a copy of the data read or written ---

    private class ByteArraySavingOutputStream(private val outputStream: OutputStream) : OutputStream() {
        val byteArrayOutputStream = ByteArrayOutputStream()
        fun getSavedBytes(): ByteArray = byteArrayOutputStream.toByteArray()
        override fun write(byte: Int) = outputStream.write(byte)
            .also { byteArrayOutputStream.write(byte) }
    }

    private class ByteArraySavingInputStream(private val inputStream: InputStream) : InputStream() {
        var byteArrayOutputStream = ByteArrayOutputStream()
        fun getSavedBytes(): ByteArray = byteArrayOutputStream.toByteArray()
        override fun read() = inputStream.read()
            .also { if (it != -1) byteArrayOutputStream.write(it) }
    }
}