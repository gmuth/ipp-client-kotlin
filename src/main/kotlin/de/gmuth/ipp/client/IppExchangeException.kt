package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppException
import de.gmuth.ipp.core.IppRequest
import de.gmuth.ipp.core.IppResponse

class IppExchangeException(
        val request: IppRequest,
        val response: IppResponse? = null,
        message: String,
        cause: Exception? = null

) : IppException(message, cause)