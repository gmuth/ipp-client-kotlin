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
        val ippRequest: IppRequest,
        val ippResponse: IppResponse,
        message: String,
        cause: Exception? = null

) : IppException(message, cause) {

    companion object {
        val log = Logging.getLogger {}
    }

    fun logDetails() {
        ippRequest.logDetails(" REQUEST: ")
        ippResponse.logDetails("RESPONSE: ")
    }

    fun saveRequestAndResponse(fileNameWithoutSuffix: String = "ipp_exchange_exception") {
        if (ippRequest.rawBytes != null) {
            val requestFile = ippRequest.saveRawBytes(File("$fileNameWithoutSuffix.request"))
            log.warn { "ipp request  written to file ${requestFile.absolutePath}" }
        }
        if (ippResponse.rawBytes != null) {
            val responseFile = ippResponse.saveRawBytes(File("$fileNameWithoutSuffix.response"))
            log.warn { "ipp response written to file ${responseFile.absolutePath}" }
        }
    }

}