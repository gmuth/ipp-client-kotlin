package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2024 Gerhard Muth
 */

import de.gmuth.ipp.core.IppRequest
import de.gmuth.ipp.core.IppStatus
import java.io.InputStream
import java.util.logging.Level
import java.util.logging.Logger

class HttpPostException(
    request: IppRequest,
    val httpStatus: Int? = null,
    val httpHeaderFields: Map<String?, List<String>>? = null,
    val httpStream: InputStream? = null,
    message: String = with(request) { "http post for request $operation to $printerOrJobUri failed" },
    cause: Throwable? = null
) : IppExchangeException(request, message, cause) {

    override fun log(logger: Logger, level: Level): Unit = with(logger) {
        super.log(logger, level)
        httpStatus?.let { log(level) { "HTTP-Status: $it" } }
        httpHeaderFields?.let { for ((key: String?, value) in it) log(level) { "HTTP-Header: $key = $value" } }
        httpStream?.let { log(level) { "HTTP-Content:\n" + it.bufferedReader().use { it.readText() } } }
        cause?.let { log(level) { "Cause: ${it.message}" } }
    }

    fun toIppOperationException(status: IppStatus) =
        IppOperationException(request, status, message ?: "", cause)

}