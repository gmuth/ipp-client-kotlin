package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppString

class IppPrinter(printerGroup: IppAttributesGroup) {

    var printerName: IppString? = null
    var printerMakeAndModel: IppString? = null
    var ippVersionsSupported: List<String>? = null
    var printColorModeSupported: List<String>? = null
    var outputModeSupported: List<String>? = null

    init {
        readFrom(printerGroup)
    }

    @Suppress("UNCHECKED_CAST")
    private fun readFrom(printerGroup: IppAttributesGroup) = with(printerGroup) {
        printerName = get("printer-name")?.value as IppString?
        printerMakeAndModel = get("printer-make-and-model")?.value as IppString?
        ippVersionsSupported = get("ipp-versions-supported")?.values as List<String>?
        printColorModeSupported = get("print-color-mode-supported")?.values as List<String>?
        outputModeSupported = get("output-mode-supported")?.values as List<String>?
    }

    fun logDetails() {
        println("PRINTER")
        logAttributeIfValueNotNull("printerName", printerName)
        logAttributeIfValueNotNull("printerMakeAndModel", printerMakeAndModel)
        logAttributeIfValueNotNull("ippVersionsSupported", ippVersionsSupported)
        logAttributeIfValueNotNull("printColorModeSupported", printColorModeSupported)
        logAttributeIfValueNotNull("outputModeSupported", outputModeSupported)
    }

    private fun logAttributeIfValueNotNull(name: String, value: Any?) {
        if (value != null) println("  $name = $value")
    }

}