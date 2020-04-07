package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.InputStream

class IppResponse(inputStream: InputStream) : IppMessage() {

    override val codeDescription: String
        get() = "status-code = $status"

    val status: IppStatus
        get() = IppStatus.fromCode(code ?: throw IppException("status-code must not be null"))

    val statusMessage: String?
        get() = operationGroup["status-message"]?.value as String?

    val operationGroup: IppAttributesGroup
        get() = getSingleAttributesGroup(IppTag.Operation)

    val jobGroup: IppAttributesGroup
        get() = getSingleAttributesGroup(IppTag.Job)

    val printerGroup: IppAttributesGroup
        get() = getSingleAttributesGroup(IppTag.Printer)

    init {
        readFrom(inputStream)
    }

}