package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2021 Gerhard Muth
 */

import de.gmuth.http.Http
import de.gmuth.http.HttpURLConnectionClient
import de.gmuth.ipp.core.*
import de.gmuth.ipp.iana.IppRegistrationsSection2
import de.gmuth.log.Logging
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger

typealias IppResponseInterceptor = (request: IppRequest, response: IppResponse) -> Unit

open class IppClient(
        val config: IppConfig = IppConfig(),
        val httpClient: Http.Client = HttpURLConnectionClient(config)
) {
    var responseInterceptor: IppResponseInterceptor? = null

    protected val requestCounter = AtomicInteger(1)

    fun basicAuth(user: String, password: String) {
        config.basicAuth = Http.BasicAuth(user, password)
    }

    companion object {
        val log = Logging.getLogger {}
    }

    //------------------------------------
    // factory/build method for IppRequest
    //------------------------------------

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

    open fun exchangeSuccessful(request: IppRequest) =
            exchange(request).apply {
                if (!isSuccessful()) {
                    IppRegistrationsSection2.validate(request)
                    val statusMessage = operationGroup.getValueOrNull("status-message") ?: IppString("")
                    throw IppExchangeException(request, this, message = "${request.operation} failed: '$status' $statusMessage")
                }
            }

    open fun exchange(request: IppRequest): IppResponse {
        val ippUri: URI = request.operationGroup.getValueOrNull("printer-uri") ?: throw IppException("missing 'printer-uri'")
        log.trace { "send '${request.operation}' request to $ippUri" }

        // convert ipp uri to http uri
        val httpUri = with(ippUri) {
            val scheme = scheme.replace("ipp", "http")
            val port = if (port == -1) 631 else port
            URI.create("$scheme://$host:$port$path")
        }

        // http post binary ipp request
        val httpResponse = httpClient.post(
                httpUri, "application/ipp",
                { httpPostStream -> request.write(httpPostStream) },
                chunked = request.hasDocument()
        ).apply {
            when {
                !isOK() -> "http request to $httpUri failed: status=$status, content-type=$contentType${textContent()}"
                !hasContentType() -> "missing content-type in http response (application/ipp required)"
                !contentType!!.startsWith("application/ipp") -> "invalid content-type: $contentType"
                else -> null
            }?.let {
                log.info { "ipp-server: $server" }
                config.logDetails()
                request.logDetails()
                throw IppExchangeException(request, null, status, message = it)
            }
        }

        // decode ipp response
        return IppResponse().apply {
            try {
                read(httpResponse.contentStream!!)
                log.debug { "$ippUri: $request => $this" }
            } catch (exception: Exception) {
                throw IppExchangeException(
                        request, this, httpResponse.status, "failed to decode ipp response", exception
                ).apply {
                    saveMessages("decoding_ipp_response_${request.requestId}_failed")
                }
            }
            if (operationGroup.containsKey("status-message")) log.debug { "status-message: $statusMessage" }
            if (containsGroup(IppTag.Unsupported)) unsupportedGroup.values.forEach {
                log.warn { "unsupported attribute: $it" }
            }
            responseInterceptor?.invoke(request, this)
        }
    }
}