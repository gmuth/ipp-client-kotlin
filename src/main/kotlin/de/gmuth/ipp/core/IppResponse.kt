package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class IppResponse : IppMessage() {

    override val codeDescription: String
        get() = "status-code = $status"

    val status: IppStatus
        get() = IppStatus.fromShort(code ?: throw IppException("status-code must not be null"))

    val statusMessage: IppString?
        get() = operationGroup.getValue("status-message")

    val operationGroup: IppAttributesGroup
        get() = getSingleAttributesGroup(IppTag.Operation)

    val jobGroup: IppAttributesGroup
        get() = getSingleAttributesGroup(IppTag.Job)

    val printerGroup: IppAttributesGroup
        get() = getSingleAttributesGroup(IppTag.Printer)

    fun isSuccessful() = code!! < 0x0100

    companion object {
        fun fromInputStream(inputStream: InputStream) = IppResponse().apply { readFrom(inputStream) }
        fun fromResource(resource: String) = IppResponse().apply { readResource(resource) }
        fun fromFile(file: File) = IppResponse().apply { readFrom(FileInputStream(file)) }
    }

}