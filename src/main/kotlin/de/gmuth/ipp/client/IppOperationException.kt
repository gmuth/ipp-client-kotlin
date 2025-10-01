package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2025 Gerhard Muth
 */

import de.gmuth.ipp.core.IppRequest
import de.gmuth.ipp.core.IppResponse
import de.gmuth.ipp.core.IppStatus
import de.gmuth.ipp.core.IppStatus.ClientErrorNotFound
import de.gmuth.ipp.core.IppTag.Operation
import java.nio.file.Path
import java.util.logging.Level
import java.util.logging.Logger

open class IppOperationException(
    request: IppRequest,
    val response: IppResponse,
    message: String = defaultMessage(request, response),
    cause: Throwable? = null
) : IppExchangeException(request, message, cause) {

    constructor(
        request: IppRequest,
        status: IppStatus,
        message: String,
        cause: Throwable? = null
    ) : this(
        request,
        IppResponse(
            status = status,
            requestId = request.requestId ?: 9999,
            statusMessageWithoutLanguage = "$message: ${cause?.message}"
        ),
        message,
        cause
    )

    class ClientErrorNotFoundException(request: IppRequest, response: IppResponse) :
        IppOperationException(request, response) {
        init {
            require(response.status == ClientErrorNotFound)
            { "IPP response status is not ClientErrorNotFound: ${response.status}" }
        }
    }

    companion object {
        fun defaultMessage(request: IppRequest, response: IppResponse) = StringBuilder().apply {
            append("${request.operation} failed")
            with(response) {
                append(": '$status'")
                if (containsGroup(Operation) && operationGroup.containsKey("status-message")) {
                    append(", $statusMessage")
                }
            }
        }.toString()
    }

    fun statusIs(status: IppStatus) = response.status == status

    override fun log(logger: Logger, level: Level) = with(logger) {
        super.log(logger, level) // logs message and request
        response.log(this, level, prefix = "RESPONSE: ")
    }

    override fun saveMessages(directory: Path, fileNameWithoutSuffix: String) {
        request.saveBytes(directory.resolve("$fileNameWithoutSuffix.req"))
        response.saveBytes(directory.resolve("$fileNameWithoutSuffix.res"))
    }

}