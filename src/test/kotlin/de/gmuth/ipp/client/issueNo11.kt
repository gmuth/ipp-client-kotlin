package de.gmuth.ipp.client

import java.net.URI
import java.util.logging.Level.SEVERE
import java.util.logging.Logger.getLogger

fun main() {
    val logger = getLogger("issueNo11")

    //val printerUri = URI.create("ipp://192.168.31.244:631/ipp/print")
    val printerUri = URI.create("ipp://xero.local/ipp/print")

    try {
        logger.info { "open ipp connection to $printerUri" }
        with(
            IppPrinter(
                printerUri,
                //httpConfig = HttpClient.Config(debugLogging = true),
                //getPrinterAttributesOnInit = false
            )
        ) {
            logger.info { "successfully connected $printerUri" }
            logger.info { toString() }
            markers.forEach { logger.info { it.toString() } }
        }
    } catch (exception: Exception) {
        logger.log(SEVERE, exception, { "failed to connect to $printerUri" })
    }
}