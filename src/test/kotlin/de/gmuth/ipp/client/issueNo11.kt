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
    var ippPrinter: IppPrinter? = null

    val printerUri = URI.create("ipp://192.168.31.244:631/ipp/print")
    //val printerUri = URI.create("ipp://xero.local/ipp/print")

    try {
        log.info { "open ipp connection to $printerUri" }
        ippPrinter = IppPrinter(
            printerUri,
            httpConfig = Http.Config(
                debugLogging = true,
                accept = null // overriden from IppClient
            ),
            getPrinterAttributesOnInit = false
        )
        with(ippPrinter) {
            with(ippClient.httpClient) {
                config.accept = null
                config.acceptEncoding = null
                log.info { config }
            }
            updateAttributes()
            logDetails()
        }
        log.info { "successfully connected $printerUri" }
    } catch (exception: Exception) {
        log.error(exception) { "failed to connect to $printerUri" }
    }
}