package de.gmuth.ipp.client

import de.gmuth.ipp.core.IppResponse
import de.gmuth.ipp.core.IppString
import de.gmuth.log.Logging
import java.io.File

/**
 * Copyright (c) 2021 Gerhard Muth
 */

class IppDocument(val job: IppJob, cupsGetDocumentResponse: IppResponse) {

    companion object {
        val log = Logging.getLogger { }
        fun getMimeTypeSuffix(mimeType: String) = when (mimeType) {
            "application/postscript" -> "ps"
            "application/pdf" -> "pdf"
            else -> "bin"
        }
    }

    // https://www.cups.org/doc/spec-ipp.html#CUPS_GET_DOCUMENT
    val attributes = cupsGetDocumentResponse.jobGroup
    val inputStream = cupsGetDocumentResponse.documentInputStream!!

    val number: Int
        get() = attributes.getValue("document-number")

    val format: String
        get() = attributes.getValue("document-format")

    val name: String
        get() = attributes.getValue("document-name")

    fun hasName() = attributes.contains("document-name")

    override fun toString() = StringBuilder().apply {
        append("document #$number ($format)")
        if (hasName()) append(" '$name'")
    }.toString()

    fun readDocument() = inputStream.readBytes().also {
        log.debug { "read ${it.size} bytes of $this" }
    }

    fun filename() = StringBuilder().apply {
        val suffix = getMimeTypeSuffix(format)
        when {
            hasName() -> append("$name.$suffix")
            job.attributes.containsKey("document-name-supplied") -> {
                append(job.attributes.getValue<IppString>("document-name-supplied").text)
            }
            else -> append("job-${job.id}-doc-$number.$suffix")
        }
    }.toString()

    fun save(file: File = File(filename())): File {
        inputStream.copyTo(file.outputStream())
        log.info { "saved ${file.length()} bytes of $this to file ${file.path}" }
        return file
    }

}