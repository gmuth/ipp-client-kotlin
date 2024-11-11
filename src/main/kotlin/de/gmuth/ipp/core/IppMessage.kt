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
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPOutputStream
import java.nio.charset.Charset as javaCharset

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

    @Suppress("UNCHECKED_CAST")
    fun <T> getAttributeValuesOrNull(groupTag: IppTag, attributeName: String) =
        if (containsGroup(groupTag)) getSingleAttributesGroup(groupTag).getValuesOrNull(attributeName) as T?
        else null

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

    @JvmOverloads
    fun write(outputStream: OutputStream, writeDocumentIfAvailable: Boolean = true) {
        val byteArraySavingOutputStream = ByteArraySavingOutputStream(outputStream)
        try {
            IppOutputStream(byteArraySavingOutputStream).writeMessage(this)
        } finally {
            rawBytes = byteArraySavingOutputStream.getSavedBytes()
        }
        if (writeDocumentIfAvailable && hasDocument()) {
            writeDocument(outputStream)
        }
    }

    fun write(file: File) =
        write(FileOutputStream(file))

    @JvmOverloads
    fun encode(appendDocumentIfAvailable: Boolean = true) = ByteArrayOutputStream().use {
        write(it, appendDocumentIfAvailable)
        it.toByteArray()
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
            if (bufferedInputStream.available() > 0) documentInputStream = bufferedInputStream
            else logger.finest { "No document bytes available from bufferedInputStream after readMessage()" }
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

    fun writeDocument(notCompressingOutputStream: OutputStream) {
        if (documentInputStreamIsConsumed) {
            throw IppException("documentInputStream is consumed")
            // write documentBytes? take care of compression!
        } else {
            val outputStream = if (operationGroup.containsKey("compression")) {
                getCompressingOutputStream(notCompressingOutputStream)
            } else {
                notCompressingOutputStream
            }
            logger.fine { "Write document using ${outputStream.javaClass.simpleName}" }
            copyUnconsumedDocumentInputStream(outputStream)
            outputStream.close() // starts optional compression
        }
    }

    private fun getCompressingOutputStream(uncompressedOutputStream: OutputStream) =
        with(operationGroup.getValueAsString("compression")) {
            when (this) {
                "none" -> uncompressedOutputStream
                "gzip" -> GZIPOutputStream(uncompressedOutputStream)
                "deflate" -> DeflaterOutputStream(uncompressedOutputStream)
                else -> throw NotImplementedError("compression '$this'")
            }
        }

    private fun copyUnconsumedDocumentInputStream(outputStream: OutputStream): Long {
        if (hasDocument() && documentInputStreamIsConsumed) {
            throw IppException("documentInputStream is consumed")
        }
        val byteArraySavingOutputStream = ByteArraySavingOutputStream(outputStream)
        return documentInputStream!!
            .copyTo(if (keepDocumentCopy) byteArraySavingOutputStream else outputStream) // number of bytes copied
            .apply {
                logger.finer { "Consumed documentInputStream: $this bytes" }
                documentInputStreamIsConsumed = true
                if (keepDocumentCopy) {
                    documentBytes = byteArraySavingOutputStream.getSavedBytes()
                    if (documentBytes!!.isNotEmpty()) logger.finer("Keeping ${documentBytes!!.size} document bytes")
                }
                byteArraySavingOutputStream.close()
            }
    }

    fun saveDocumentBytes(file: File) = file.run {
        if (documentBytes == null || documentBytes!!.isEmpty()) throw IppException("No documentBytes available")
        outputStream().use { ByteArrayInputStream(documentBytes).copyTo(it)}
        logger.info { "Saved ${length()} document bytes to $path" }
    }

    fun writeBytes(outputStream: OutputStream) =
        if (rawBytes == null) throw IppException("No raw bytes to write. You must call read/decode or write/encode before.")
        else outputStream.write(rawBytes)

    fun saveBytes(file: File) = file.apply {
        outputStream().use { writeBytes(it) }
        logger.info { "Saved $path (${length()} bytes)" }
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