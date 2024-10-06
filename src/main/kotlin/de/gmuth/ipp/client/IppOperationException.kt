package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2024 Gerhard Muth
 */

import de.gmuth.ipp.core.IppRequest
import de.gmuth.ipp.core.IppResponse
import de.gmuth.ipp.core.IppStatus
import de.gmuth.ipp.core.IppStatus.ClientErrorNotFound
import de.gmuth.ipp.core.IppString
import de.gmuth.ipp.core.IppTag.Operation
import java.io.File
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
        messageLanguage: String = "en",
        cause: Throwable? = null
    ) : this(
        request,
        IppResponse(status, statusMessage = IppString("$message: ${cause?.message}", messageLanguage)),
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

    override fun saveMessages(
        fileNameWithoutSuffix: String,
        directory: String
    ) {
        request.saveBytes(File(directory, "$fileNameWithoutSuffix.request"))
        response.saveBytes(File(directory, "$fileNameWithoutSuffix.response"))
    }

}