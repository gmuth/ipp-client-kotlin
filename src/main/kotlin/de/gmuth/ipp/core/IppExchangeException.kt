package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class IppExchangeException(
        val ippRequest: IppRequest,
        val ippResponse: IppResponse,
        message: String,
        cause: Exception? = null

) : IppException(message, cause) {

    fun logDetails() {
        ippRequest.logDetails("IPP-REQUEST: ")
        ippResponse.logDetails("IPP-RESPONSE: ")
    }

}