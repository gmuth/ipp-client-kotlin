package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppString

class IppPrinter(printerGroup: IppAttributesGroup) {

    var printerState: IppPrinterState? = null
    var printerName: IppString? = null
    var printerMakeAndModel: IppString? = null
    var ippVersionsSupported: List<String>? = null
    var ippFeaturesSupported: List<String>? = null
    var printColorModeSupported: List<String>? = null
    var outputModeSupported: List<String>? = null

    init {
        readFrom(printerGroup)
    }

    companion object {
        val requestAttributes = listOf(
                "printer-state",
                "printer-name",
                "printer-make-and-model",
                "ipp-versions-supported",
                "ipp-features-supported",
                "print-color-mode-supported",
                "output-mode-supported"
        )
    }

    fun readFrom(printerGroup: IppAttributesGroup) = with(printerGroup) {
        printerState = IppPrinterState.fromCode(getValue("printer-state") as Int)
        printerName = getValue("printer-name")
        printerMakeAndModel = getValue("printer-make-and-model")
        ippVersionsSupported = getValues("ipp-versions-supported")
        ippFeaturesSupported = getValues("ipp-features-supported")
        printColorModeSupported = getValues("print-color-mode-supported")
        outputModeSupported = getValues("output-mode-supported")
    }

    fun logDetails() {
        println("PRINTER")
        logAttributeIfValueNotNull("printerState", printerState)
        logAttributeIfValueNotNull("printerName", printerName)
        logAttributeIfValueNotNull("printerMakeAndModel", printerMakeAndModel)
        logAttributeIfValueNotNull("ippVersionsSupported", ippVersionsSupported)
        logAttributeIfValueNotNull("ippFeaturesSupported", ippFeaturesSupported)
        logAttributeIfValueNotNull("printColorModeSupported", printColorModeSupported)
        logAttributeIfValueNotNull("outputModeSupported", outputModeSupported)
    }

    private fun logAttributeIfValueNotNull(name: String, value: Any?) {
        if (value != null) println("  $name = $value")
    }

}