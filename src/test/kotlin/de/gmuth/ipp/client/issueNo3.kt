package de.gmuth.ipp.client

import de.gmuth.log.Logging
import java.net.HttpURLConnection
import java.net.URI

fun main() {

    //Logging.defaultLogLevel = Logging.LogLevel.DEBUG
    val log = Logging.getLogger {}

    var ippPrinter: IppPrinter? = null
    var httpConnection: HttpURLConnection? = null

    var printerUri = URI.create("http://xero.local:631/ipp/print")
    printerUri = URI.create("http://BRWD812655BA041.local:631/ipp/print")

    val printerUrl = URI("http://${printerUri.host}:${printerUri.port}").toURL()
    try {
        log.info { "open http connection to $printerUrl" }
        httpConnection = printerUrl.openConnection() as HttpURLConnection
        with(httpConnection) {
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

    try {
        log.info { "open ipp connection to $printerUri" }
        ippPrinter = IppPrinter(printerUri)
        log.info { "successfully connected $printerUri" }
    } catch (exception: Exception) {
        log.error(exception) { "failed to connect to $printerUri" }
    }

    if (ippPrinter != null) with(ippPrinter) {
        try {
            log.info { "documentFormatSupported:" }
            documentFormatSupported.forEach { log.info { " $it" } }
        } catch (exception: Exception) {
            log.error(exception) { "failed to read documentFormatSupported" }
        }
        try {
            savePrinterAttributes()
            log.info { "saved printer attributes" }
        } catch (exception: Exception) {
            log.error(exception) { "failed to save printer attributes" }
        }
    }

}