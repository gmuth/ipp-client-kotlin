package de.gmuth.ipp.client

import de.gmuth.ipp.core.IppException
import de.gmuth.ipp.core.IppRequest
import de.gmuth.ipp.core.IppResponse

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
        ippRequest.logDetails(" REQUEST: ")
        ippResponse.logDetails("RESPONSE: ")
    }

}