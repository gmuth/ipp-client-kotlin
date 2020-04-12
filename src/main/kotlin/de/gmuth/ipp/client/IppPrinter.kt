package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppPrinterState
import de.gmuth.ipp.core.IppString

class IppPrinter(printerGroup: IppAttributesGroup) {

    var ippVersionsSupported: List<String>? = null
    var printerState: IppPrinterState? = null
    var printerName: IppString? = null
    var printerMakeAndModel: IppString? = null
    var printColorModeSupported: List<String>? = null
    var outputModeSupported: List<String>? = null

    init {
        readFrom(printerGroup)
    }

    @Suppress("UNCHECKED_CAST")
    fun readFrom(printerGroup: IppAttributesGroup) = with(printerGroup) {
        ippVersionsSupported = get("ipp-versions-supported")?.values as List<String>?
        printerState = get("printer-state")?.value as IppPrinterState?
        printerName = get("printer-name")?.value as IppString?
        printerMakeAndModel = get("printer-make-and-model")?.value as IppString?
        printColorModeSupported = get("print-color-mode-supported")?.values as List<String>?
        outputModeSupported = get("output-mode-supported")?.values as List<String>?
    }

    fun logDetails() {
        println("PRINTER")
        logAttributeIfValueNotNull("ippVersionsSupported", ippVersionsSupported)
        logAttributeIfValueNotNull("printerState", printerState)
        logAttributeIfValueNotNull("printerName", printerName)
        logAttributeIfValueNotNull("printerMakeAndModel", printerMakeAndModel)
        logAttributeIfValueNotNull("printColorModeSupported", printColorModeSupported)
        logAttributeIfValueNotNull("outputModeSupported", outputModeSupported)
    }

    private fun logAttributeIfValueNotNull(name: String, value: Any?) {
        if (value != null) println("  $name = $value")
    }

}