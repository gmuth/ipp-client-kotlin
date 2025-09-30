package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2025 Gerhard Muth
 */

import de.gmuth.ipp.core.IppException
import de.gmuth.ipp.core.IppRequest
import java.nio.file.Path
import java.util.logging.Level
import java.util.logging.Level.INFO
import java.util.logging.Logger
import kotlin.io.path.createTempDirectory

open class IppExchangeException(
    val request: IppRequest,
    message: String = "Exchanging request '${request.operation}' failed",
    cause: Throwable? = null
) : IppException(message, cause) {

    open fun log(logger: Logger, level: Level = INFO): Unit = with(logger) {
        log(level) { message }
        request.log(this, level, prefix = "REQUEST: ")
    }

    open fun saveMessages(
        directory: Path = createTempDirectory("ipp-client-"),
        fileNameWithoutSuffix: String = "ipp_exchange_exception"
    ) =
        request.saveBytes(directory.resolve("$fileNameWithoutSuffix.request"))

}