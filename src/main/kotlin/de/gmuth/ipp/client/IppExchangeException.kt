package de.gmuth.ipp.client

import de.gmuth.ipp.core.IppException
import de.gmuth.ipp.core.IppRequest
import de.gmuth.ipp.core.IppResponse

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class IppExchangeException(val request: IppRequest, val response: IppResponse, message: String) : IppException(message)