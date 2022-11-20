package de.gmuth.ipp.client

import de.gmuth.http.HttpURLConnectionClient
import de.gmuth.log.Logging
import java.net.HttpURLConnection
import java.net.URI

fun main() {

    val printerUri = URI.create("ipp://xero.local:631/ipp/print")

    Logging.defaultLogLevel = Logging.LogLevel.DEBUG
    HttpURLConnectionClient.log.logLevel = Logging.LogLevel.TRACE
    val log = Logging.getLogger {}
    var ippPrinter: IppPrinter? = null

    // httpConnect(printerUri)
    val saveAttributes = false

    val ippConfig = IppConfig().apply {
        ippVersion = "1.1"
        logDetails()
    }
    try {
        log.info { "open ipp connection to $printerUri" }
        ippPrinter = IppPrinter(printerUri, ippConfig = ippConfig, getPrinterAttributesOnInit = true)
        log.info { "successfully connected $printerUri" }
    } catch (exception: Exception) {
        log.error(exception) { "failed to connect to $printerUri" }
    }

    if (ippPrinter != null) ippPrinter.run {
        if (saveAttributes) {
            try {
                savePrinterAttributes()
                log.info { "saved printer attributes" }
            } catch (exception: Exception) {
                log.error(exception) { "failed to save printer attributes" }
            }
        }
        try {
            log.info { "documentFormatSupported:" }
            documentFormatSupported.forEach { log.info { " $it" } }
        } catch (exception: Exception) {
            log.error(exception) { "failed to read documentFormatSupported" }
        }
    }
}

fun httpConnect(printerUri: URI) {
    val log = Logging.getLogger {}
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
        log.error(exception) { "http connection failed to $printerUrl" }
    }
}