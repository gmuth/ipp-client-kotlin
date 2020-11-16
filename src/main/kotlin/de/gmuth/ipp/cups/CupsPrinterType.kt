package de.gmuth.ipp.cups

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class CupsPrinterType(val value: Int) {

    fun toSet(): Set<CupsPrinterCapability> = CupsPrinterCapability
            .values()
            .filter { (value shr it.bit) and 1 == 1 }
            .toSet()

    fun contains(capability: CupsPrinterCapability) = toSet().contains(capability)

    override fun toString() = "$value (${toSet().joinToString(",")})"

    fun logDetails() {
        println(String.format("PRINTER-TYPE 0x%08X capabilities:", value))
        for (capability in toSet()) {
            println(" - ${capability.description}")
        }
    }

    companion object {
        fun fromInt(value: Int?): CupsPrinterType? = if (value == null) null else CupsPrinterType(value)
    }

}