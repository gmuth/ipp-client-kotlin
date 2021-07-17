package de.gmuth.ipp.client

import de.gmuth.ipp.core.IppException
import de.gmuth.ipp.core.IppRequest
import de.gmuth.ipp.core.IppResponse
import de.gmuth.log.Logging
import java.io.File

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class IppExchangeException(
        val request: IppRequest,
        val response: IppResponse? = null,
        val httpStatus: Int? = null,
        message: String,
        cause: Exception? = null

) : IppException(message, cause) {

    companion object {
        val log = Logging.getLogger {}
    }

    fun logDetails() {
        if (httpStatus != null) log.info { "HTTP-STATUS: $httpStatus" }
        request.logDetails(" REQUEST: ")
        response?.logDetails("RESPONSE: ")
    }

    fun saveRequestAndResponse(fileNameWithoutSuffix: String = "ipp_exchange_exception") {
        val requestFile = request.saveRawBytes(File("$fileNameWithoutSuffix.request"))
        log.warn { "ipp request  written to file ${requestFile.absolutePath}" }
        response?.let {
            val responseFile = it.saveRawBytes(File("$fileNameWithoutSuffix.response"))
            log.warn { "ipp response written to file ${responseFile.absolutePath}" }
        }
    }

}