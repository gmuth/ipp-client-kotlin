package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class IppResponse : IppMessage() {

    override val codeDescription: String
        get() = "$status"

    val status: IppStatus
        get() = IppStatus.fromShort(code!!)

    val statusMessage: IppString
        get() = operationGroup.getValue("status-message")

    val printerGroup: IppAttributesGroup
        get() = getSingleAttributesGroup(IppTag.Printer)

    val jobGroup: IppAttributesGroup
        get() = getSingleAttributesGroup(IppTag.Job)

    fun isSuccessful() =
            status.isSuccessful()

}