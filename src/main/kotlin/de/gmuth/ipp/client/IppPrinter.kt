package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppIntegerTime
import de.gmuth.ipp.core.IppString
import de.gmuth.ipp.cups.CupsPrinterType

class IppPrinter(printerGroup: IppAttributesGroup) {

    var printerState: IppPrinterState? = null
    var printerName: IppString? = null
    var printerMakeAndModel: IppString? = null
    var ippVersionsSupported: List<String>? = null
    var ippFeaturesSupported: List<String>? = null
    var printColorModeSupported: List<String>? = null
    var outputModeSupported: List<String>? = null
    var printerUpTime: IppIntegerTime? = null
    var printerType: CupsPrinterType? = null
    var jobCreationAttributesSupported: List<String>? = null

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
                "output-mode-supported",
                "printer-up-time",
                "printer-type",
                "job-creation-attributes-supported"
        )
    }

    fun readFrom(printerGroup: IppAttributesGroup) = with(printerGroup) {
        printerState = IppPrinterState.fromInt(getValue("printer-state") as Int)
        printerName = getValue("printer-name")
        printerMakeAndModel = getValue("printer-make-and-model")
        ippVersionsSupported = getValues("ipp-versions-supported")
        ippFeaturesSupported = getValues("ipp-features-supported")
        printColorModeSupported = getValues("print-color-mode-supported")
        outputModeSupported = getValues("output-mode-supported")
        printerUpTime = IppIntegerTime.fromInt(getValue("printer-up-time") as Int?)
        printerType = CupsPrinterType.fromInt(getValue("printer-type") as Int?)
        jobCreationAttributesSupported = getValues("job-creation-attributes-supported")
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
        logAttributeIfValueNotNull("printerUpTime", printerUpTime)
        logAttributeIfValueNotNull("printerType", printerType)
        logAttributeIfValueNotNull("jobCreationAttributesSupported", jobCreationAttributesSupported)
    }

    private fun logAttributeIfValueNotNull(name: String, value: Any?) {
        if (value != null) println("  $name = $value")
    }

}