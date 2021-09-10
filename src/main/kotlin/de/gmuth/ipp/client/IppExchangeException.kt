package de.gmuth.ipp.client

import de.gmuth.ipp.core.IppException
import de.gmuth.ipp.core.IppRequest
import de.gmuth.ipp.core.IppResponse
import de.gmuth.ipp.core.IppStatus
import de.gmuth.log.Logging
import java.io.File

/**
 * Copyright (c) 2020-2021 Gerhard Muth
 */

class IppExchangeException(
        val request: IppRequest,
        val response: IppResponse? = null,
        val httpStatus: Int? = null,
        message: String = defaultMessage(request, response),
        cause: Exception? = null

) : IppException(message, cause) {

    companion object {
        val log = Logging.getLogger {}
        fun defaultMessage(request: IppRequest, response: IppResponse?) = StringBuilder().apply {
            append("${request.operation} failed")
            response?.run {
                append(": '$status'")
                if (hasStatusMessage()) append(", $statusMessage")
            }
        }.toString()
    }

    init {
        if (httpStatus == 400) saveMessages("http_400")
    }

    fun statusIs(status: IppStatus) = response?.status == status

    fun logDetails() {
        if (httpStatus != null) log.info { "HTTP-STATUS: $httpStatus" }
        request.logDetails(" REQUEST: ")
        response?.logDetails("RESPONSE: ")
    }

    fun saveMessages(fileNameWithoutSuffix: String = "ipp_exchange_exception") {
        request.saveRawBytes(File("$fileNameWithoutSuffix.request"))
        response?.saveRawBytes(File("$fileNameWithoutSuffix.response"))
    }

}