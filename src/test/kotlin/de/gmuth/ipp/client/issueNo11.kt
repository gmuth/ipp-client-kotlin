package de.gmuth.ipp.client

import de.gmuth.http.Http
import de.gmuth.http.HttpURLConnectionClient
import de.gmuth.log.Logging
import java.net.URI

fun main() {
    //Logging.defaultLogLevel = Logging.LogLevel.DEBUG
    HttpURLConnectionClient.log.logLevel = Logging.LogLevel.TRACE
    IppClient.log.logLevel = Logging.LogLevel.DEBUG

    val log = Logging.getLogger {}

    val printerUri = URI.create("ipp://192.168.31.244:631/ipp/print")
    //val printerUri = URI.create("ipp://xero.local/ipp/print")

    try {
        log.info { "open ipp connection to $printerUri" }
        with(
            IppPrinter(
                printerUri,
                // omit accept and accept-encoding (null!)
                httpConfig = Http.Config(debugLogging = true),
                //getPrinterAttributesOnInit = false
            )
        ) {
            //ippClient.httpConfig.debugLogging = true
            //updateAttributes()
            log.info { "successfully connected $printerUri" }
            //logDetails()
            markers.forEach { log.info { it } }
        }
    } catch (exception: Exception) {
        log.error(exception) { "failed to connect to $printerUri" }
    }
}