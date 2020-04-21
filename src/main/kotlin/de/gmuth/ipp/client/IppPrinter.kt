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

    val isAcceptingJobs: Boolean
        get() = attributes.getValue("printer-is-accepting-jobs")

    val state: IppPrinterState
        get() = IppPrinterState.fromInt(attributes.getValue("printer-state") as Int)

    val stateReasons: List<String>
        get() = attributes.getValues("printer-state-reasons")

    override fun toString() =
            "IppPrinter: name = $name, makeAndModel = $makeAndModel, state = $state, stateReasons = ${stateReasons.joinToString(",")}"

    fun logDetails() {
        println("PRINTER-$name ($makeAndModel), $state (${stateReasons.joinToString (",")})")
        for (attribute in attributes.values) {
            println("  $attribute")
        }
    }

}