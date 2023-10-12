package de.gmuth.ipp.client

/**
 * Copyright (c) 2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttributesGroup
import java.util.logging.Level
import java.util.logging.Logger

abstract class IppObject(
    private val exchangeDelegate: IppExchange,
    internal val attributes: IppAttributesGroup
) : IppExchange by exchangeDelegate {

    protected abstract fun objectName(): String

    @JvmOverloads
    fun log(logger: Logger, level: Level = Level.INFO) =
        attributes.log(logger, level, title = objectName())

}