package de.gmuth.ipp.client

import de.gmuth.ipp.core.IppException
import de.gmuth.log.Logging
import java.net.URI
import java.util.logging.Level
import java.util.logging.Logger

fun main() {
    Logging.configure()
    val logger = Logger.getLogger("main")

    val printerURI = URI.create("ipp://xero.local")
    try {
        IppInspector.inspect(printerURI, cancelJob = true)
    } catch (ippException: IppException) {
        logger.log(Level.SEVERE, "Failed to inspect printer $printerURI", ippException)
    }
}