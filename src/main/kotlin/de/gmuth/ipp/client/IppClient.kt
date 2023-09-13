package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.http.Http
import de.gmuth.ipp.client.IppExchangeException.ClientErrorNotFoundException
import de.gmuth.ipp.core.IppException
import de.gmuth.ipp.core.IppOperation
import de.gmuth.ipp.core.IppRequest
import de.gmuth.ipp.core.IppResponse
import de.gmuth.ipp.core.IppStatus.ClientErrorBadRequest
import de.gmuth.ipp.core.IppStatus.ClientErrorNotFound
import de.gmuth.ipp.core.IppTag.Unsupported
import de.gmuth.ipp.iana.IppRegistrationsSection2
import java.io.File
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level.FINE
import java.util.logging.Level.WARNING
import java.util.logging.Logger.getLogger

typealias IppResponseInterceptor = (request: IppRequest, response: IppResponse) -> Unit

open class IppClient(
    val config: IppConfig = IppConfig(),
    val httpConfig: Http.Config = Http.Config(),
    val httpClient: Http.Client = Http.defaultImplementation.createClient(httpConfig)
) {
    val log = getLogger(javaClass.name)
    var saveMessages: Boolean = false
    var saveMessagesDirectory = File("ipp-messages")
    var responseInterceptor: IppResponseInterceptor? = null

    fun basicAuth(user: String, password: String) {
        httpConfig.basicAuth = Http.BasicAuth(user, password)
        config.userName = user
    }

    companion object {
        const val APPLICATION_IPP = "application/ipp"
        const val version = "3.0-SNAPSHOT"
        const val build = "09-2023"

        init {
            println("IPP-Client: Version: $version, Build: $build, MIT License, (c) 2020-2023 Gerhard Muth")
        }
    }

    init {
        with(httpConfig) { if (userAgent == null) userAgent = "ipp-client/$version" }
    }

    private var httpServer: String? = null

    @SuppressWarnings("kotlin:S6512") // read only
    fun getHttpServer() = httpServer

    //-----------------
    // build IppRequest
    //-----------------

    private val requestCounter = AtomicInteger(1)

    fun ippRequest(
        operation: IppOperation,
        printerUri: URI,
        jobId: Int? = null,
        requestedAttributes: List<String>? = null,
        userName: String? = config.userName
    ) = IppRequest(
        operation,
        printerUri,
        jobId,
        requestedAttributes,
        userName,
        config.ippVersion,
        requestCounter.getAndIncrement(),
        config.charset,
        config.naturalLanguage
    )

    //------------------------------------
    // exchange IppRequest for IppResponse
    //------------------------------------

    open fun exchange(request: IppRequest, throwWhenNotSuccessful: Boolean = true): IppResponse {
        val ippUri: URI = request.printerUri
        val httpUri = toHttpUri(ippUri)
        log.finer { "send '${request.operation}' request to $ippUri" }

        val httpResponse = httpPostRequest(httpUri, request)
        val response = decodeIppResponse(request, httpResponse)
        log.fine { "$ippUri: $request => $response" }
        httpServer = httpResponse.server

        if (saveMessages) {
            val messageSubDirectory = File(saveMessagesDirectory, ippUri.host).apply {
                if (!mkdirs() && !isDirectory) throw IppException("failed to create directory: $path")
            }

            fun file(suffix: String) = File(messageSubDirectory, "${request.requestId}-${request.operation}.$suffix")
            request.saveRawBytes(file("request"))
            response.saveRawBytes(file("response"))
        }

        responseInterceptor?.invoke(request, response)

        if (!response.isSuccessful()) {
            IppRegistrationsSection2.validate(request)
            if (throwWhenNotSuccessful)
                throw if (response.status == ClientErrorNotFound) ClientErrorNotFoundException(request, response)
                else IppExchangeException(request, response)
        }
        return response
    }

    fun toHttpUri(ippUri: URI): URI = with(ippUri) {
        val scheme = scheme.replace("ipp", "http")
        val port = if (port == -1) 631 else port
        URI.create("$scheme://$host:$port$rawPath")
    }

    fun httpPostRequest(httpUri: URI, request: IppRequest) = httpClient.post(
        httpUri, APPLICATION_IPP,
        { httpPostStream -> request.write(httpPostStream) },
        chunked = request.hasDocument()
    ).apply {
        var exceptionMessage: String? = null
        if (contentType == null) {
            log.fine { "missing content-type in http response (should be '$APPLICATION_IPP')" }
        } else {
            if (!contentType.startsWith(APPLICATION_IPP)) {
                exceptionMessage = "invalid content-type: $contentType (expecting '$APPLICATION_IPP')"
            }
        }
        if (status != 200) exceptionMessage = "http request to $httpUri failed: status=$status"
        if (status == 401) exceptionMessage = with(request) {
            "user '$requestingUserName' is unauthorized for operation '$operation' (status=$status)"
        }
        exceptionMessage?.run {
            config.log(log, WARNING)
            request.log(log, WARNING, prefix = "IPP REQUEST: ")
            log.warning { "http response status: $status" }
            server?.let { log.warning { "ipp-server: $it" } }
            contentType?.let { log.warning { "content-type: $it" } }
            contentStream?.let { log.warning { "content:\n" + it.bufferedReader().use { it.readText() } } }
            throw IppExchangeException(request, null, status, message = exceptionMessage)
        }
    }

    fun decodeIppResponse(request: IppRequest, httpResponse: Http.Response) = IppResponse().apply {
        try {
            read(httpResponse.contentStream!!)
        } catch (exception: Exception) {
            throw IppExchangeException(
                request, this, httpResponse.status, "failed to decode ipp response", exception
            ).apply {
                saveMessages("decoding_ipp_response_${request.requestId}_failed")
            }
        }
        if (status == ClientErrorBadRequest) request.log(log, FINE, prefix="BAD-REQUEST: ")
        if (!status.isSuccessful()) log.fine { "status: $status" }
        if (hasStatusMessage()) log.fine { "status-message: $statusMessage" }
        if (containsGroup(Unsupported)) unsupportedGroup.values.forEach {
            log.warning { "unsupported: $it" }
        }
    }
}
