package de.gmuth.ipp.client

import java.net.HttpURLConnection
import java.net.URI
import java.util.logging.Level.SEVERE
import java.util.logging.Logger.getLogger

fun main() {

    val printerUri = URI.create("ipp://xero.local:631/ipp/print")

    //ConsoleLogger.defaultLogLevel = Logging.LogLevel.DEBUG
    //HttpURLConnectionClient.log.logLevel = Logging.LogLevel.TRACE
    val logger = getLogger("issueNo3")
    var ippPrinter: IppPrinter? = null

    // httpConnect(printerUri)
    val saveAttributes = false

    val ippConfig = IppConfig().apply {
        ippVersion = "1.1"
        log(logger)
    }
    try {
        logger.info { "open ipp connection to $printerUri" }
        ippPrinter = IppPrinter(printerUri, ippConfig = ippConfig, getPrinterAttributesOnInit = true)
        logger.info { "successfully connected $printerUri" }
    } catch (exception: Exception) {
        logger.log(SEVERE, exception, { "failed to connect to $printerUri" })
    }

    if (ippPrinter != null) ippPrinter.run {
        if (saveAttributes) {
            try {
                savePrinterAttributes()
                logger.info { "saved printer attributes" }
            } catch (exception: Exception) {
                logger.log(SEVERE, exception, { "failed to save printer attributes" })
            }
        }
        try {
            logger.info { "documentFormatSupported:" }
            documentFormatSupported.forEach { logger.info { " $it" } }
        } catch (exception: Exception) {
            logger.log(SEVERE, exception, { "failed to read documentFormatSupported" })
        }
    }
}

fun httpConnect(printerUri: URI) {
    val log = getLogger("httpConnect")
    val printerUrl = URI("http://${printerUri.host}:${printerUri.port}").toURL()
    try {
        log.info { "open http connection to $printerUrl" }
        with(printerUrl.openConnection() as HttpURLConnection) {
            log.info { "response: $responseCode $responseMessage" }
            val contentResponseStream = try {
                inputStream
            } catch (exception: Exception) {
                errorStream
            }
            val contentBytes = contentResponseStream.readBytes()
            log.info { "content: ${contentBytes.size} bytes of type '$contentType'" }
        }
    } catch (exception: Exception) {
        log.log(SEVERE, exception, { "http connection failed to $printerUrl" })
    }
}