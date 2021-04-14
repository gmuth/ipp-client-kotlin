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

    val statusMessage: IppString // RFC 8011 4.1.6.2
        get() = operationGroup.getValue("status-message")

    val printerGroup: IppAttributesGroup
        get() = getSingleAttributesGroup(IppTag.Printer)

    val unsupportedGroup: IppAttributesGroup
        get() = getSingleAttributesGroup(IppTag.Unsupported)

    fun isSuccessful() =
            status.isSuccessful()

}