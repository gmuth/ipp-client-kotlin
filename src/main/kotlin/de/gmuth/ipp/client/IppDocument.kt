package de.gmuth.ipp.client

import de.gmuth.ipp.core.IppException
import de.gmuth.ipp.core.IppResponse
import de.gmuth.ipp.core.IppString
import de.gmuth.log.Logging
import java.io.File

/**
 * Copyright (c) 2021-2022 Gerhard Muth
 */

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
        with(job) {
            append("job-$id")
            if (numberOfDocuments > 1) append("-doc-$number")
            if (attributes.contains("job-originating-user-name")) append("-$originatingUserName")
            if (attributes.contains("job-name")) append("-${name.text}")
        }
        append(".${filenameSuffix()}")
        toString()
    }

    fun save(
        directory: File = job.printer.printerDirectory(),
        file: File = File(directory, filename()),
        overwrite: Boolean = true
    ) = file.also {
        if (file.isFile && !overwrite) throw IppException("File '$it' already exists")
        inputStream.copyTo(it.outputStream())
        log.info { "saved ${it.length()} bytes of $this to file ${it.path}" }
    }

    override fun toString() = StringBuilder("document #$number ($format)").run {
        if (attributes.contains("document-name")) append(" '$name'")
        toString()
    }

    fun logDetails() = attributes.logDetails(title = "DOCUMENT-$number")

}