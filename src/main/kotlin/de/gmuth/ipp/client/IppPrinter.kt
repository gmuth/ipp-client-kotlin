package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppString

data class IppPrinter(var attributes: IppAttributesGroup) {

    val name: IppString
        get() = attributes.getValue("printer-name")

    val makeAndModel: IppString
        get() = attributes.getValue("printer-make-and-model")

    override fun toString(): String {
        return "IppPrinter: name = $name, makeAndModel = $makeAndModel"
    }

    fun logDetails() {
        println("PRINTER-$name ($makeAndModel)")
        for (attribute in attributes.values) {
            println("  $attribute")
        }
    }

}