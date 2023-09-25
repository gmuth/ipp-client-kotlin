package de.gmuth.ipp.client

/**
 * Copyright (c) 2021-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppString
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.logging.Logger
import java.util.logging.Logger.getLogger
import kotlin.io.path.createTempDirectory

class IppDocument(
    val job: IppJob,
    val attributes: IppAttributesGroup,
    val inputStream: InputStream
) {
    val log = getLogger(javaClass.name)

    val number: Int
        get() = attributes.getValue("document-number")

    val format: String
        get() = attributes.getValue("document-format")

    val name: IppString
        get() = attributes.getValue("document-name")

    var file: File? = null

    fun readBytes() = inputStream.readBytes().also {
        log.fine { "read ${it.size} bytes of $this" }
    }

    fun filenameSuffix() = when (format) {
        "application/octetstream" -> "bin"
        "application/postscript" -> "ps"
        "application/pdf" -> "pdf"
        "image/jpeg" -> "jpg"
        "text/plain" -> "txt"
        else -> format.split("/")[1]
    }

    fun filename() = StringBuilder().run {
        var suffix: String? = filenameSuffix()
        with(job) {
            append("job-$id")
            if (numberOfDocuments > 1) append("-doc-$number")
            job.getOriginatingUserNameOrAppleJobOwnerOrNull()?.let { append("-$it") }
            if (attributes.containsKey("com.apple.print.JobInfo.PMApplicationName")) {
                append("-${applePrintJobInfo.applicationName}")
            }
            job.getJobNameOrDocumentNameSuppliedOrAppleJobNameOrNull()?.let {
                append("-${it.take(100)}")
                if (it.lowercase().endsWith(".$suffix")) suffix = null
            }
        }
        suffix?.let { append(".$it") }
        toString().replace('/', '_')
    }

    fun save(
        directory: File = createTempDirectory().toFile(),
        file: File = File(directory, filename()),
        overwrite: Boolean = true
    ) = file.also {
        if (file.isFile && !overwrite) throw IOException("File '$file' already exists")
        inputStream.copyTo(file.outputStream())
        this.file = file
        log.info { "Saved $this" }
    }

    fun runCommand(commandToHandleFile: String) =
        Runtime.getRuntime().exec(arrayOf(commandToHandleFile, file!!.absolutePath))

    override fun toString() = StringBuilder().run {
        append("document #$number ($format) of job #${job.id}")
        if (attributes.containsKey("document-name")) append(" '$name'")
        if (file != null) append(": $file (${file!!.length()} bytes)")
        toString()
    }

    fun log(logger: Logger) =
        attributes.log(logger, title = "DOCUMENT-$number")

}