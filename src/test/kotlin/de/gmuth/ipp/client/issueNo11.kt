package de.gmuth.ipp.client

import java.net.URI
import java.util.logging.Level.SEVERE
import java.util.logging.Logger.getLogger

fun main() {
    //Logging.defaultLogLevel = Logging.LogLevel.DEBUG
    //HttpURLConnectionClient.log.logLevel = Logging.LogLevel.TRACE
    //IppClient.log.logLevel = Logging.LogLevel.DEBUG

    val log = getLogger("issueNo11")

    //val printerUri = URI.create("ipp://192.168.31.244:631/ipp/print")
    val printerUri = URI.create("ipp://xero.local/ipp/print")

    try {
        log.info { "open ipp connection to $printerUri" }
        with(
            IppPrinter(
                printerUri,
                //httpConfig = HttpClient.Config(debugLogging = true),
                //getPrinterAttributesOnInit = false
            )
        ) {
            log.info { "successfully connected $printerUri" }
            log.info { toString() }
            markers.forEach { log.info { it.toString() } }
        }
    } catch (exception: Exception) {
        log.log(SEVERE, exception, { "failed to connect to $printerUri" })
    }
}