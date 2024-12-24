package de.gmuth.ipp.client

/**
 * Copyright (c) 2024 Gerhard Muth
 */

import de.gmuth.ipp.client.IppDocument.Companion.getDocumentFormatFilenameExtension
import de.gmuth.ipp.core.IppRequest
import de.gmuth.ipp.core.IppResponse
import java.io.File
import java.io.File.separator
import java.io.IOException
import java.time.LocalDateTime.now
import java.time.format.DateTimeFormatter.ofPattern
import java.util.logging.Logger

class IppRequestExchangedEvent(val request: IppRequest, val response: IppResponse) {

    private val logger = Logger.getLogger(IppRequestExchangedEvent::class.qualifiedName)

    override fun toString() =
        "#%04d %-60s = #%04d %s".format(request.requestId, request, response.requestId, response)

    fun save(
        directory: File,
        maxFilenameLength: Int = 200,
        saveEvent: Boolean = false,
        saveDocument: Boolean = false,
        saveRawMessages: Boolean = true
    ) {
        logger.fine("Save files in ${directory.path}")
        try {
            val connectionDirectory = File(
                directory,
                request.connectionName().replace(separator, "_")
            ).createDirectoryIfNotExists()

            fun filename(extension: String) = StringBuilder().run {
                append(ofPattern("HHmmssSSS").format(now()))
                append(" #%04d".format(request.requestId))
                append(" $request = $response")
                toString()
                    .take(maxFilenameLength - 1 - extension.length)
                    .plus(".$extension")
                    .replace(separator, "_")
            }

            fun file(extension: String) =
                File(connectionDirectory, filename(extension))

            // Save raw message bytes
            if (saveRawMessages) {
                logger.fine { "Save raw IPP messages" }
                request.saveBytes(file("req"))
                response.saveBytes(file("res"))
            }

            // Save decoded request and response to single text file
            if (saveEvent) file("txt").run {
                printWriter().use {
                    request.writeText(it, "File: $name")
                    response.writeText(it)
                    it.println("---------------------------------------------------------------------")
                    request.httpUserAgent?.run { it.println("UserAgent: $this") }
                    response.httpServer?.run { it.println("Server: $this") }
                }
                logger.fine("Saved $name (${length()} bytes)")
            }

            // Save document
            if (saveDocument && request.hasDocument()) {
                logger.fine { "Save document" }
                val filenameExtension = with(request) {
                    if (operationGroup.containsKey("document-format")) getDocumentFormatFilenameExtension() else "bin"
                }
                request.saveDocument(file(filenameExtension))
            }

        } catch (throwable: Throwable) {
            logger.severe("Failed to save: $throwable")
        }
    }

    private fun IppRequest.getDocumentFormatFilenameExtension() =
        getDocumentFormatFilenameExtension(operationGroup)

    private fun File.createDirectoryIfNotExists() = this
        .apply { if (!mkdirs() && !isDirectory) throw IOException("Failed to create directory: $path") }

}