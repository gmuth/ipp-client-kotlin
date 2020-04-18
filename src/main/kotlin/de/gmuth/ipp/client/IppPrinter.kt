package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppIntegerTime
import de.gmuth.ipp.core.IppString
import de.gmuth.ipp.cups.CupsPrinterType
import de.gmuth.ipp.iana.IppRegistrationsSection6

class IppPrinter(printerGroup: IppAttributesGroup) {

    var printerIsAcceptingJobs: Boolean? = null
    var printerState: IppPrinterState? = null
    var printerName: IppString? = null
    var printerMakeAndModel: IppString? = null
    var ippVersionsSupported: List<String>? = null
    var ippFeaturesSupported: List<String>? = null
    var printColorModeSupported: List<String>? = null
    var outputModeSupported: List<String>? = null
    var printerUpTime: IppIntegerTime? = null
    var printerType: CupsPrinterType? = null
    var operationsSupported: List<String>? = null // IppOperation not used to allow unknown non-enum values
    var documentFormatSupported: List<String>? = null
    var jobCreationAttributesSupported: List<String>? = null

    init {
        readFrom(printerGroup)
    }

    companion object {
        val requestAttributes = listOf(
                "printer-is-accepting-jobs",
                "printer-state",
                "printer-name",
                "printer-make-and-model",
                "ipp-versions-supported",
                "ipp-features-supported",
                "print-color-mode-supported",
                "output-mode-supported",
                "printer-up-time",
                "printer-type",
                "operations-supported",
                "document-format-supported",
                "job-creation-attributes-supported"
        )
    }

    fun readFrom(printerGroup: IppAttributesGroup) = with(printerGroup) {
        printerIsAcceptingJobs = getValue("printer-is-accepting-jobs")
        printerState = IppPrinterState.fromInt(getValue("printer-state") as Int)
        printerName = getValue("printer-name")
        printerMakeAndModel = getValue("printer-make-and-model")
        ippVersionsSupported = getValues("ipp-versions-supported")
        ippFeaturesSupported = getValues("ipp-features-supported")
        printColorModeSupported = getValues("print-color-mode-supported")
        outputModeSupported = getValues("output-mode-supported")
        printerUpTime = IppIntegerTime.fromInt(getValue("printer-up-time") as Int?)
        printerType = CupsPrinterType.fromInt(getValue("printer-type") as Int?)
        operationsSupported = (getValues("operations-supported") as List<Int>)
                .map { IppRegistrationsSection6.getOperationsSupportedValueName(it).toString() }
        documentFormatSupported = getValues("document-format-supported")
        jobCreationAttributesSupported = getValues("job-creation-attributes-supported")
    }

    fun logDetails() {
        println("PRINTER")
        logAttributeIfValueNotNull("printerIsAcceptingJobs", printerIsAcceptingJobs)
        logAttributeIfValueNotNull("printerState", printerState)
        logAttributeIfValueNotNull("printerName", printerName)
        logAttributeIfValueNotNull("printerMakeAndModel", printerMakeAndModel)
        logAttributeIfValueNotNull("ippVersionsSupported", ippVersionsSupported)
        logAttributeIfValueNotNull("ippFeaturesSupported", ippFeaturesSupported)
        logAttributeIfValueNotNull("printColorModeSupported", printColorModeSupported)
        logAttributeIfValueNotNull("outputModeSupported", outputModeSupported)
        logAttributeIfValueNotNull("printerUpTime", printerUpTime)
        logAttributeIfValueNotNull("printerType", printerType)
        logAttributeIfValueNotNull("operationsSupported", operationsSupported)
        logAttributeIfValueNotNull("documentFormatSupported", documentFormatSupported)
        logAttributeIfValueNotNull("jobCreationAttributesSupported", jobCreationAttributesSupported)
    }

    private fun logAttributeIfValueNotNull(name: String, value: Any?) {
        if (value != null) println("  $name = $value")
    }

}