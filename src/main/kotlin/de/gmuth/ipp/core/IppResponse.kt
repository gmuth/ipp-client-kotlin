package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class IppResponse : IppMessage() {

    override val codeDescription: String
        get() = "status-code = $status"

    val status: IppStatus
        get() = IppStatus.fromShort(code ?: throw IppException("status-code must not be null"))

    val statusMessage: IppString?
        get() = operationGroup.getValue("status-message")

    val jobGroup: IppAttributesGroup
        get() = getSingleAttributesGroup(IppTag.Job)

    val printerGroup: IppAttributesGroup
        get() = getSingleAttributesGroup(IppTag.Printer)

    fun isSuccessful() = status.isSuccessful()

}