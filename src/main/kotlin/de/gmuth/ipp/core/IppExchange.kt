package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2021 Gerhard Muth
 */

// transport interface for clients and
// request handling interface for servers

interface IppExchange {

    fun exchange(request: IppRequest): IppResponse

}