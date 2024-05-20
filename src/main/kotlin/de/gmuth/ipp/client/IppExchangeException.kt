package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2024 Gerhard Muth
 */

import de.gmuth.ipp.core.IppException
import de.gmuth.ipp.core.IppRequest
import java.io.File
import java.util.logging.Level
import java.util.logging.Level.INFO
import java.util.logging.Logger
import kotlin.io.path.createTempDirectory
import kotlin.io.path.pathString

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
        fileNameWithoutSuffix: String = "ipp_exchange_exception",
        directory: String = createTempDirectory("ipp-client-").pathString
    ) {
        request.saveBytes(File(directory, "$fileNameWithoutSuffix.request"))
    }

}