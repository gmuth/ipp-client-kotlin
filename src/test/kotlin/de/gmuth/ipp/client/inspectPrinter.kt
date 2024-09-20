package de.gmuth.ipp.client

import de.gmuth.log.Logging
import java.net.URI
import java.util.logging.Level
import java.util.logging.Logger

fun main() {
    Logging.configure()
    val logger = Logger.getLogger("main")

    val printerURI = URI.create("ipp://xero.local")
    try {
        IppInspector().inspect(printerURI, cancelJob = true)
    } catch (throwable: Throwable) {
        logger.log(Level.SEVERE, "Failed to inspect printer $printerURI", throwable)
    }
}