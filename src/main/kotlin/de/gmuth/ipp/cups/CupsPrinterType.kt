package de.gmuth.ipp.cups

import de.gmuth.log.Log

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class CupsPrinterType(val value: Int) {

    companion object {
        val log = Log.getWriter("CupsPrinterType", Log.Level.INFO)
    }

    fun toSet(): Set<CupsPrinterCapability> =
            CupsPrinterCapability
                    .values()
                    .filter { (value shr it.bit) and 1 == 1 }
                    .toSet()

    fun contains(capability: CupsPrinterCapability) =
            toSet().contains(capability)

    override fun toString() =
            "$value (${toSet().joinToString(",")})"

    fun logDetails() {
        log.info { String.format("PRINTER-TYPE 0x%08X capabilities:", value) }
        for (capability in toSet()) {
            log.info { " - ${capability.description}" }
        }
    }

}