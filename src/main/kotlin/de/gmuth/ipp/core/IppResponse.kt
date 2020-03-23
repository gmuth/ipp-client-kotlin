package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.InputStream

class IppResponse : IppMessage() {

    val status: IppStatus
        get() = IppStatus.fromShort(code ?: throw IllegalArgumentException("status-code must not be null"))

    var statusMessage: String? = null

    override fun getCodeDescription() = "status-code = $status"

    override fun readFrom(inputStream: InputStream): String? {
        statusMessage = super.readFrom(inputStream)
        return statusMessage
    }

    companion object {
        fun ofInputStream(inputStream: InputStream) = IppResponse().apply { readFrom(inputStream) }
    }

}