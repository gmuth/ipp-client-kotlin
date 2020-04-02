package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class IppExchangeException(val request: IppRequest, val response: IppResponse, message: String) : IppException(message)