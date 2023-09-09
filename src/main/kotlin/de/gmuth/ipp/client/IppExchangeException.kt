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
import java.util.logging.Logger
import java.util.logging.Logger.getLogger
import kotlin.io.path.createTempDirectory
import kotlin.io.path.pathString

open class IppExchangeException(
    val request: IppRequest,
    val response: IppResponse? = null,
    val httpStatus: Int? = null,
    message: String = defaultMessage(request, response),
    cause: Exception? = null

) : IppException(message, cause) {

    class ClientErrorNotFoundException(request: IppRequest, response: IppResponse) :
        IppExchangeException(request, response) {
        init {
            require(response.status == ClientErrorNotFound) { "ipp response status is not ClientErrorNotFound: ${response.status}" }
        }
    }

    val log = getLogger(javaClass.name)

    companion object {
        fun defaultMessage(request: IppRequest, response: IppResponse?) = StringBuilder().apply {
            append("${request.operation} failed")
            response?.run {
                append(": '$status'")
                if (hasStatusMessage()) append(", $statusMessage")
            }
        }.toString()
    }

    init {
        if (httpStatus == 400) saveMessages("http_400")
    }

    fun statusIs(status: IppStatus) = response?.status == status

    fun log(logger: Logger) = logger.run {
        if (httpStatus != null) info { "HTTP-STATUS: $httpStatus" }
        request.logDetails(" REQUEST: ")
        response?.logDetails("RESPONSE: ")
    }

    fun saveMessages(
        fileNameWithoutSuffix: String = "ipp_exchange_exception",
        directory: String = createTempDirectory("ipp-client-").pathString
    ) {
        request.saveRawBytes(File(directory, "$fileNameWithoutSuffix.request"))
        response?.saveRawBytes(File(directory, "$fileNameWithoutSuffix.response"))
    }

}