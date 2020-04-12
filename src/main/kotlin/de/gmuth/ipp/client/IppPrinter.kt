package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttributesGroup
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

    fun readFrom(printerGroup: IppAttributesGroup) = with(printerGroup) {
        ippVersionsSupported = getValues("ipp-versions-supported")
        printerState = IppPrinterState.fromCode(getValue("printer-state") as Int)
        printerName = getValue("printer-name")
        printerMakeAndModel = getValue("printer-make-and-model")
        printColorModeSupported = getValues("print-color-mode-supported")
        outputModeSupported = getValues("output-mode-supported")
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