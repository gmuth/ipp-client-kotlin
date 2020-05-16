package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.net.URI

interface IppExchange {

    @Throws(IppExchangeException::class)
    fun exchange(ippUri: URI, ippRequest: IppRequest): IppResponse

}