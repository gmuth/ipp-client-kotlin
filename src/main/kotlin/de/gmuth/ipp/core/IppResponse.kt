package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.InputStream

class IppResponse : IppMessage() {

    override val codeDescription: String
        get() = "status-code = $status"

    val status: IppStatus
        get() = IppStatus.fromCode(code ?: throw IppException("status-code must not be null"))

    var statusMessage: String? = null

    val jobGroup: IppAttributesGroup
        get() = getSingleAttributesGroup(IppTag.Job)

    companion object {
        fun fromInputStream(inputStream: InputStream) = IppResponse().apply { statusMessage = readFrom(inputStream) }
    }

}