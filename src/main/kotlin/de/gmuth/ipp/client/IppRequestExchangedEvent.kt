package de.gmuth.ipp.client

/**
 * Copyright (c) 2024-2025 Gerhard Muth
 */

import de.gmuth.ipp.client.IppDocument.Companion.getDocumentFormatFilenameExtension
import de.gmuth.ipp.core.IppRequest
import de.gmuth.ipp.core.IppResponse
import java.io.File
import java.io.PrintWriter
import java.net.URI
import java.nio.file.Files
import java.nio.file.Files.newBufferedWriter
import java.nio.file.Path
import java.time.LocalDateTime.now
import java.time.format.DateTimeFormatter.ofPattern
import java.util.logging.Level
import java.util.logging.Level.INFO
import java.util.logging.Logger
import kotlin.io.path.createDirectories
import kotlin.io.path.inputStream

class IppRequestExchangedEvent(
    val request: IppRequest,
    val response: IppResponse,
    val uri: URI? = null
) {

    constructor(requestPath: Path, responsePath: Path) : this(
        IppRequest().apply { read(requestPath.inputStream()) },
        IppResponse().apply { read(responsePath.inputStream()) }
    )

    private val logger = Logger.getLogger(IppRequestExchangedEvent::class.qualifiedName)

    override fun toString() =
        "#%04d %-60s = #%04d %s".format(request.requestId, request, response.requestId, response)

    fun log(logger: Logger, level: Level = INFO) {
        logger.log(level, "-".repeat(50) + " request" + if (uri == null) "" else " to $uri")
        request.log(logger, level)
        logger.log(level, "-".repeat(50) + " response" + if (uri == null) "" else " from $uri")
        response.log(logger, level)
    }

    fun save(
        directory: Path,
        saveEvent: Boolean = false,
        saveDocument: Boolean = false,
        saveRawMessages: Boolean = true,
        maxFilenameLength: Int = 200
    ) {
        logger.fine("Save files in $directory")
        try {
            val connectionDirectory = directory.resolve(
                request.connectionName()
                    .replace(File.separator, "_")
                    .replace(":", "_")
            )

            fun filename(extension: String) = StringBuilder().run {
                append(ofPattern("HHmmssSSS").format(now()))
                append(" #%04d".format(request.requestId))
                append(" $request = $response")
                toString()
                    .take(maxFilenameLength - 1 - extension.length)
                    .plus(".$extension")
                    .replace(File.separator, "_")
            }

            fun fileWithExtension(extension: String) =
                connectionDirectory.resolve(filename(extension))

            // Save raw message bytes
            if (saveRawMessages) {
                logger.fine { "Save raw IPP messages" }
                request.saveBytes(fileWithExtension("req"))
                response.saveBytes(fileWithExtension("res"))
            }

            // Save decoded request and response to single text file
            if (saveEvent) fileWithExtension("txt").run {
                parent?.createDirectories()
                newBufferedWriter(this).use {
                    val printWriter = PrintWriter(it)
                    request.writeText(printWriter, "File: $this")
                    response.writeText(printWriter)
                    printWriter.println("---------------------------------------------------------------------")
                    request.httpUserAgent?.run { printWriter.println("UserAgent: $this") }
                    response.httpServer?.run { printWriter.println("Server: $this") }
                }
                logger.fine("Saved ${toAbsolutePath()} (${Files.size(this)} bytes)")
            }

            // Save document
            if (saveDocument && request.hasDocument()) {
                logger.fine { "Save document" }
                val filenameExtension = with(request) {
                    if (operationGroup.containsKey("document-format")) getDocumentFormatFilenameExtension() else "bin"
                }
                request.saveDocument(fileWithExtension(filenameExtension))
            }

        } catch (throwable: Throwable) {
            logger.severe("Failed to save: $throwable")
        }
    }

    private fun IppRequest.getDocumentFormatFilenameExtension() =
        getDocumentFormatFilenameExtension(operationGroup)

}