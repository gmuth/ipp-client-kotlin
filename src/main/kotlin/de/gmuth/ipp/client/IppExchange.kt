package de.gmuth.ipp.client

import de.gmuth.ipp.core.IppRequest
import de.gmuth.ipp.core.IppResponse

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

fun interface IppExchange {

    fun exchange(request: IppRequest): IppResponse

}