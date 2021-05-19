package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class IppResponse : IppMessage() {

    override val codeDescription: String
        get() = status.toString()

    var status: IppStatus
        get() = IppStatus.fromShort(code!!)
        set(ippStatus) {
            code = ippStatus.code
        }

    // https://datatracker.ietf.org/doc/html/rfc8011#page-42
    val statusMessage: IppString
        get() = operationGroup.getValue("status-message")

    val printerGroup: IppAttributesGroup
        get() = getSingleAttributesGroup(IppTag.Printer)

    val unsupportedGroup: IppAttributesGroup
        get() = getSingleAttributesGroup(IppTag.Unsupported)

    fun isSuccessful() =
            status.isSuccessful()

}