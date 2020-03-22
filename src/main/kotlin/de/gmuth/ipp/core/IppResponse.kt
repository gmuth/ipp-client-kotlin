package de.gmuth.ipp.core

import java.io.ByteArrayInputStream
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
        fun ofByteArray(byteArray: ByteArray) = ofInputStream(ByteArrayInputStream(byteArray))
    }

}