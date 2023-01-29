package de.gmuth.ipp.client

/**
 * Copyright (c) 2021-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppException
import de.gmuth.ipp.core.IppResponse
import de.gmuth.ipp.core.IppString
import de.gmuth.log.Logging
import java.io.File
import kotlin.io.path.createTempDirectory

class IppDocument(val job: IppJob, cupsGetDocumentResponse: IppResponse) {

    companion object {
        val log = Logging.getLogger { }
    }

    // https://www.cups.org/doc/spec-ipp.html#CUPS_GET_DOCUMENT
    val attributes = cupsGetDocumentResponse.jobGroup
    val inputStream = cupsGetDocumentResponse.documentInputStream!!

    val number: Int
        get() = attributes.getValue("document-number")

    val format: String
        get() = attributes.getValue("document-format")

    val name: IppString
        get() = attributes.getValue("document-name")

    fun readBytes() = inputStream.readBytes().also {
        log.debug { "read ${it.size} bytes of $this" }
    }

    fun filenameSuffix() = when (format) {
        "application/postscript" -> "ps"
        "application/pdf" -> "pdf"
        else -> "bin"
    }

    fun filename() = StringBuilder().run {
        var suffix: String? = filenameSuffix()
        with(job) {
            append("job-$id")
            numberOfDocuments?.let { if (it > 1) append("-doc-$number") }
            if (attributes.containsKey("job-originating-user-name")) append("-$originatingUserName")
            if (attributes.containsKey("job-name")) {
                append("-${name.text}")
                if (name.text.endsWith(".$suffix")) suffix = null
            }
        }
        suffix?.let { append(".$it") }
        toString()
    }

    fun save(
        directory: File = createTempDirectory().toFile(),
        file: File = File(directory, filename()),
        overwrite: Boolean = true
    ) = file.also {
        if (file.isFile && !overwrite) throw IppException("File '$it' already exists")
        inputStream.copyTo(it.outputStream())
        log.info { "saved $this: $it (${it.length()} bytes)" }
    }

    fun delete(directory: File) {
        val file = File(directory, filename())
        val action = if (file.delete()) "deleted" else "failed to delete"
        log.info { "$action $this: $file" }
    }

    override fun toString() = StringBuilder("document #$number ($format)").run {
        if (attributes.containsKey("document-name")) append(" '$name'")
        toString()
    }

    fun logDetails() = attributes.logDetails(title = "DOCUMENT-$number")

}