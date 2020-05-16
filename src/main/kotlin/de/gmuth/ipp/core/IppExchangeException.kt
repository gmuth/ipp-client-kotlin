package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class IppExchangeException(
        val request: IppRequest,
        val response: IppResponse,
        message: String,
        cause: Exception? = null

) : IppException(message, cause)