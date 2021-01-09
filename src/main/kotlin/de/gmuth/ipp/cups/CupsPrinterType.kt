package de.gmuth.ipp.cups

import de.gmuth.log.Logging
import de.gmuth.log.Logging.LogLevel

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class CupsPrinterType(val value: Int) {

    fun toSet(): Set<CupsPrinterCapability> =
            CupsPrinterCapability
                    .values()
                    .filter { (value shr it.bit) and 1 == 1 }
                    .toSet()

    fun contains(capability: CupsPrinterCapability) =
            toSet().contains(capability)

    override fun toString() =
            "$value (${toSet().joinToString(",")})"

    fun logDetails(logger: Logging.Logger, logLevel: LogLevel = logger.logLevel) {
        logger.log(logLevel) { String.format("PRINTER-TYPE 0x%08X capabilities:", value) }
        for (capability in toSet()) {
            logger.log(logLevel) { " - ${capability.description}" }
        }
    }

}