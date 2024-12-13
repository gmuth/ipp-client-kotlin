package de.gmuth.ipp.client

/**
 * Copyright (c) 2021-2024 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppException
import de.gmuth.ipp.core.IppString
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.Logger.getLogger

@Suppress("kotlin:S1192")
class IppDocument(
    val job: IppJob,
    val attributes: IppAttributesGroup,
    private val inputStream: InputStream
) {

    companion object {
        fun getFilenameExtension(mediaType: String) = when (mediaType) {
            "application/postscript", "application/vnd.cups-postscript", "application/vnd.adobe-reader-postscript" -> "ps"
            "application/pdf", "application/vnd.cups-pdf" -> "pdf"
            "application/octet-stream" -> "bin"
            "text/plain" -> "txt"
            else -> mediaType.split("/")[1]
        }
        fun getDocumentFormatFilenameExtension(attributes: IppAttributesGroup) =
            getFilenameExtension(attributes.getValue("document-format"))
    }

    private val logger = getLogger(javaClass.name)

    val number: Int
        get() = attributes.getValue("document-number")

    val format: String
        get() = attributes.getValue("document-format")

    val name: IppString
        get() = attributes.getValue("document-name")

    var file: File? = null

    fun readBytes() = inputStream.readBytes()
        .also { logger.fine { "Read ${it.size} bytes of $this" } }

    fun filename() = StringBuilder().apply {
        var extension: String? = getFilenameExtension(format)
        job.run {
            append("job-$id")
            getNumberOfDocumentsOrDocumentCount().let { if (it > 1) append("-doc-$it") }
            getOriginatingUserNameOrAppleJobOwnerOrNull()?.let { append("-$it") }
            if (attributes.containsKey("com.apple.print.JobInfo.PMApplicationName")) {
                append("-${attributes.getValue<String>("com.apple.print.JobInfo.PMApplicationName")}")
            }
            getJobNameOrDocumentNameSuppliedOrAppleJobNameOrNull()?.run {
                append("-${take(100)}")
                if (lowercase().endsWith(".$extension")) extension = null
            }
        }
        extension?.let { append(".$it") }
    }.toString().replace(File.separator, "_")

    fun copyTo(outputStream: OutputStream) =
        inputStream.copyTo(outputStream)

    fun save(
        directory: File = job.printer.printerDirectory,
        filename: String = filename(),
        overwrite: Boolean = true
    ) = File(directory, filename).also {
        if (!directory.exists()) directory.mkdirs()
        if (it.isFile && !overwrite) throw IOException("File '$it' already exists")
        copyTo(it.outputStream())
        this.file = it
        logger.info { "Saved $file ${if (attributes.containsKey("document-format")) "($format)" else ""}" }
    }

    fun runtimeExecCommand(commandToHandleFile: String) =
        if (file == null) throw IppException("Missing file to handle.")
        else Runtime.getRuntime().exec(arrayOf(commandToHandleFile, file!!.absolutePath))

    override fun toString() = StringBuilder("Document #$number").run {
        append(" ($format) of job #${job.id}")
        if (attributes.containsKey("document-name")) append(" '$name'")
        if (file != null) append(": $file (${file!!.length()} bytes)")
        toString()
    }

    @JvmOverloads
    fun log(logger: Logger, level: Level = Level.INFO) =
        attributes.log(logger, level, title = "DOCUMENT #$number")
}