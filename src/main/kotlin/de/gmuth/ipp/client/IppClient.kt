package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2022 Gerhard Muth
 */

import de.gmuth.http.Http
import de.gmuth.ipp.core.IppException
import de.gmuth.ipp.core.IppOperation
import de.gmuth.ipp.core.IppRequest
import de.gmuth.ipp.core.IppResponse
import de.gmuth.ipp.core.IppStatus.ClientErrorBadRequest
import de.gmuth.ipp.core.IppStatus.SuccessfulOk
import de.gmuth.ipp.core.IppTag.Unsupported
import de.gmuth.ipp.iana.IppRegistrationsSection2
import de.gmuth.log.Logging
import java.io.File
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger

typealias IppResponseInterceptor = (request: IppRequest, response: IppResponse) -> Unit

open class IppClient(
    val config: IppConfig = IppConfig(),
    val httpClient: Http.Client = Http.defaultImplementation.createClient(Http.Config())
) {
    var saveMessages: Boolean = false
    var saveMessagesDirectory = File("ipp-messages")
    var responseInterceptor: IppResponseInterceptor? = null

    val httpConfig: Http.Config
        get() = httpClient.config

    fun basicAuth(user: String, password: String) {
        httpConfig.basicAuth = Http.BasicAuth(user, password)
    }

    companion object {
        val log = Logging.getLogger {}
        const val APPLICATION_IPP = "application/ipp"
        const val version = "2.4-SNAPSHOT"
        const val build = "2022"

        init {
            println("IPP-Client: Version: $version, Build: $build, MIT License, (c) 2020-2022 Gerhard Muth")
        }
    }

    init {
        httpConfig.apply {
            userAgent = "ipp-client/$version"
            acceptEncoding = "identity" // avoids 'gzip' with Android's OkHttp
            accept = APPLICATION_IPP // avoids 'text/html' with sun.net.www.protocol.http.HttpURLConnection
        }
    }

    //-----------------
    // build IppRequest
    //-----------------

    private val requestCounter = AtomicInteger(1)

    fun ippRequest(
        operation: IppOperation,
        printerUri: URI,
        jobId: Int? = null,
        requestedAttributes: List<String>? = null
    ) = IppRequest(
        operation,
        printerUri,
        jobId,
        requestedAttributes,
        config.userName,
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
        log.trace { "send '${request.operation}' request to $ippUri" }

        val httpResponse = httpPostRequest(httpUri, request)
        val response = decodeIppResponse(request, httpResponse)
        log.debug { "$ippUri: $request => $response" }

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
            if (throwWhenNotSuccessful) throw IppExchangeException(request, response)
        }
        return response
    }

    fun toHttpUri(ippUri: URI) = with(ippUri) {
        val scheme = scheme.replace("ipp", "http")
        val port = if (port == -1) 631 else port
        URI.create("$scheme://$host:$port$path")
    }

    fun httpPostRequest(httpUri: URI, request: IppRequest) = httpClient.post(
        httpUri, APPLICATION_IPP,
        { httpPostStream -> request.write(httpPostStream) },
        chunked = request.hasDocument()
    ).apply {
        when { // http response is not successful
            !isOK() -> "http request to $httpUri failed: status=$status, content-type=$contentType${textContent()}"
            !hasContentType() -> "missing content-type in http response (application/ipp required)"
            !contentType!!.startsWith(APPLICATION_IPP) -> "invalid content-type: $contentType"
            else -> null
        }?.let {
            server?.run { log.info { "ipp-server: $server" } }
            config.logDetails()
            request.logDetails()
            throw IppExchangeException(request, null, status, message = it)
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
        if (status == ClientErrorBadRequest) request.logDetails("BAD-REQUEST: ")
        if (status != SuccessfulOk) log.warn { "status: $status" }
        if (hasStatusMessage()) log.debug { "status-message: $statusMessage" }
        if (containsGroup(Unsupported)) unsupportedGroup.values.forEach {
            log.warn { "unsupported: $it" }
        }
    }
}