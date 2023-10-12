package de.gmuth.ipp.client

/**
 * Copyright (c) 2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppRequest
import java.util.logging.Level
import java.util.logging.Logger

abstract class IppObject(
    private val exchangeDelegate: IppExchange,
    internal val attributes: IppAttributesGroup
) : IppExchange {

    override fun exchange(request: IppRequest) =
        exchangeDelegate.exchange(request)

    protected abstract fun objectName(): String

    @JvmOverloads
    fun log(logger: Logger, level: Level = Level.INFO) =
        attributes.log(logger, level, title = objectName())

}