package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttributesGroup

class IppPrinter(printerGroup: IppAttributesGroup) {

    var printerName: String? = null
    var printerMakeAndModel: String? = null
    var ippVersionsSupported: List<String>? = null
    var printColorModeSupported: List<String>? = null
    var outputModeSupported: List<String>? = null

    init {
        readFrom(printerGroup)
    }

    private fun readFrom(printerGroup: IppAttributesGroup) = with(printerGroup) {
        printerName = get("printer-name")?.value as String?
        printerMakeAndModel = get("printer-make-and-model")?.value as String?
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