package de.gmuth.ipp.core

import java.io.File

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class IppResponse() : IppMessage() {

    constructor(file: File) : this() {
        read(file)
    }

    override val codeDescription: String
        get() = "$status"

    var status: IppStatus
        get() = IppStatus.fromShort(code!!)
        set(ippStatus) {
            code = ippStatus.code
        }

    val statusMessage: IppString
        get() = operationGroup.getValue("status-message")

    val printerGroup: IppAttributesGroup
        get() = getSingleAttributesGroup(IppTag.Printer)

    val jobGroup: IppAttributesGroup
        get() = getSingleAttributesGroup(IppTag.Job)

    val unsupportedGroup: IppAttributesGroup
        get() = getSingleAttributesGroup(IppTag.Unsupported)

    fun isSuccessful() =
            status.isSuccessful()

}