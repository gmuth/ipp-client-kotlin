package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppTag.*
import java.io.*
import java.util.logging.Level
import java.util.logging.Level.INFO
import java.util.logging.Logger
import java.util.logging.Logger.getLogger

abstract class IppMessage() {

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

    internal fun getAttributesGroups(tag: IppTag) =
        attributesGroups.filter { it.tag == tag }

    internal fun getSingleAttributesGroup(tag: IppTag) = getAttributesGroups(tag).run {
        if (isEmpty()) throw IppException("No group found with tag '$tag' in $attributesGroups")
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
        val byteArrayOutputStream = ByteArrayOutputStream()
        val byteArraySavingOutputStream = object : OutputStream() {
            override fun write(byte: Int) = outputStream.write(byte)
                .also { byteArrayOutputStream.write(byte) }
        }
        try {
            IppOutputStream(byteArraySavingOutputStream).writeMessage(this)
        } finally {
            rawBytes = byteArrayOutputStream.toByteArray()
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
        logger.fine { "Read file ${file.absolutePath}: ${file.length()} bytes" }
        read(FileInputStream(file))
    }

    fun decode(byteArray: ByteArray) {
        logger.fine { "Decode ${byteArray.size} bytes" }
        read(ByteArrayInputStream(byteArray))
    }

    // ------------------------
    // DOCUMENT and IPP-MESSAGE
    // ------------------------

    protected fun copyDocumentStream(outputStream: OutputStream): Long {
        if (documentInputStreamIsConsumed) logger.warning { "documentInputStream is consumed" }
        return documentInputStream!!.copyTo(outputStream).apply {
            logger.fine { "consumed documentInputStream: $this bytes" }
            documentInputStreamIsConsumed = true
        }
    }

    fun saveDocumentStream(file: File) {
        copyDocumentStream(file.outputStream())
        logger.info { "saved ${file.length()} document bytes to file ${file.path}" }
    }

    fun saveBytes(file: File) =
        if (rawBytes == null) {
            throw IppException("No raw bytes to save. You must call read/decode or write/encode before.")
        } else {
            file.writeBytes(rawBytes!!)
            logger.info { "Saved ${file.path} (${file.length()} bytes)" }
        }

    protected fun write(bufferedWriter: BufferedWriter, title: String) {
        fun writeln(text: String) = bufferedWriter.run { write(text); newLine() }
        bufferedWriter.write(title)
        if (rawBytes != null) bufferedWriter.write(" (decoded ${rawBytes!!.size} raw IPP bytes)")
        bufferedWriter.newLine()
        writeln("version $version")
        writeln("$codeDescription")
        writeln("request-id $requestId")
        for (group in attributesGroups) {
            group.write(bufferedWriter)
        }
    }

    fun saveText(file: File) = file.apply {
        bufferedWriter().use { write(it, title = "# File: ${file.name}") }
        logger.info { "Saved $path (${length()} bytes)" }
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
    fun log(logger: Logger, level: Level = INFO, prefix: String = "") {
        if (rawBytes != null) logger.log(level) { "${prefix}${rawBytes!!.size} raw ipp bytes" }
        logger.log(level) { "${prefix}version = $version" }
        logger.log(level) { "${prefix}$codeDescription" }
        logger.log(level) { "${prefix}request-id = $requestId" }
        for (group in attributesGroups) {
            group.log(logger, level, prefix = prefix)
        }
    }

}