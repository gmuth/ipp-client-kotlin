package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppException
import de.gmuth.ipp.core.IppRequest
import de.gmuth.ipp.core.IppResponse
import de.gmuth.ipp.core.IppStatus
import de.gmuth.ipp.core.IppStatus.ClientErrorNotFound
import java.io.File
import java.io.InputStream
import java.util.logging.Level
import java.util.logging.Level.INFO
import java.util.logging.Logger
import java.util.logging.Logger.getLogger
import kotlin.io.path.createTempDirectory
import kotlin.io.path.pathString

open class IppExchangeException(
    val request: IppRequest,
    val response: IppResponse? = null,
    val httpStatus: Int? = null,
    val httpHeaderFields: Map<String?, List<String>>? = null,
    val httpStream: InputStream? = null,
    message: String = defaultMessage(request, response),
    cause: Throwable? = null,
) : IppException(message, cause) {

    class ClientErrorNotFoundException(request: IppRequest, response: IppResponse) :
        IppExchangeException(request, response) {
        init {
            require(response.status == ClientErrorNotFound)
            { "ipp response status is not ClientErrorNotFound: ${response.status}" }
        }
    }

    val log = getLogger(javaClass.name)

    companion object {
        fun defaultMessage(request: IppRequest, response: IppResponse?) = StringBuilder().run {
            append("${request.operation} failed")
            response?.run {
                append(": '$status'")
                if (operationGroup.containsKey("status-message")) append(", $statusMessage")
            }
            toString()
        }
    }

    fun statusIs(status: IppStatus) = response?.status == status

    fun log(logger: Logger, level: Level = INFO) = logger.run {
        log(level) { message }
        request.log(log, level, prefix = "REQUEST: ")
        response?.log(log, level, prefix = "RESPONSE: ")
        httpStatus?.let { log(level) { "HTTP-Status: $it" } }
        httpHeaderFields?.let { for ((key: String?, value) in it) log(level) { "HTTP: $key = $value" } }
        httpStream?.let { log.log(level) { "HTTP-Content:\n" + it.bufferedReader().use { it.readText() } } }
    }

    fun saveMessages(
        fileNameWithoutSuffix: String = "ipp_exchange_exception_$httpStatus",
        directory: String = createTempDirectory("ipp-client-").pathString
    ) {
        request.saveRawBytes(File(directory, "$fileNameWithoutSuffix.request"))
        response?.saveRawBytes(File(directory, "$fileNameWithoutSuffix.response"))
    }

}