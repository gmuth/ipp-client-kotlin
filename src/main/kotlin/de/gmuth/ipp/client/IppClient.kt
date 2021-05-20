package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.http.Http
import de.gmuth.http.HttpURLConnectionClient
import de.gmuth.http.SSLHelper
import de.gmuth.ipp.core.*
import de.gmuth.log.Logging
import java.net.URI
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicInteger

open class IppClient(
        var ippVersion: String = "1.1",
        val httpClient: Http.Client = HttpURLConnectionClient(),
        var httpBasicAuth: Http.BasicAuth? = null,
        val requestingUserName: String? = if (httpBasicAuth != null) httpBasicAuth.user else System.getProperty("user.name")
) {
    var requestCharset: Charset = Charsets.UTF_8
    var requestNaturalLanguage: String = "en"

    protected val requestCounter = AtomicInteger(1)

    fun trustAnyCertificate() {
        httpClient.config.sslSocketFactory = SSLHelper.sslSocketFactoryForAnyCertificate()
    }

    fun basicAuth(user: String, password: String) {
        httpBasicAuth = Http.BasicAuth(user, password)
    }

    companion object {
        val log = Logging.getLogger {}
        val ippContentType = "application/ipp"
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
            requestingUserName,
            ippVersion,
            requestCounter.getAndIncrement(),
            requestCharset,
            requestNaturalLanguage
    )

    //------------------------------------
    // exchange IppRequest for IppResponse
    //------------------------------------

    fun exchangeSuccessful(ippRequest: IppRequest) = exchange(ippRequest).apply {
        if (!isSuccessful()) {
            val statusMessage = operationGroup.getValueOrNull("status-message") ?: IppString("")
            throw IppExchangeException(ippRequest, this, "${ippRequest.operation} failed: '$status' $statusMessage")
        }
    }

    fun exchange(ippRequest: IppRequest): IppResponse {
        val ippUri: URI = ippRequest.operationGroup.getValueOrNull("printer-uri") ?: throw IppException("missing 'printer-uri'")
        log.trace { "send '${ippRequest.operation}' request to $ippUri" }

        // convert ipp uri to http uri
        val httpUri = with(ippUri) {
            val scheme = scheme.replace("ipp", "http")
            val port = if (port == -1) 631 else port
            URI.create("$scheme://$host:$port$path")
        }

        // http post binary ipp message or throw exception
        val httpResponse = httpClient.postSuccessful(
                httpUri,
                ippContentType,
                { httpPostStream -> ippRequest.write(httpPostStream) },
                httpBasicAuth
        )
        with(httpResponse) {
            log.trace { "ipp-server: $server" }
            if (!contentType!!.startsWith(ippContentType)) throw IppException("invalid content-type: $contentType")
        }

        // decode ipp response
        val ippResponse = IppResponse()
        try {
            ippResponse.read(httpResponse.contentStream!!)
        } catch (exception: Exception) {
            throw IppExchangeException(ippRequest, ippResponse, "failed to decode ipp response", exception).apply {
                saveRequestAndResponse("ipp_decoding_failed")
            }
        }

        // response logging
        log.debug { "$ippUri: $ippRequest => $ippResponse" }
        with(ippResponse) {
            if (operationGroup.containsKey("status-message")) {
                log.debug { "status-message: ${ippResponse.statusMessage}" }
            }
            if (containsGroup(IppTag.Unsupported)) {
                unsupportedGroup.values.forEach { log.warn { "unsupported attribute: $it" } }
            }
        }

        return ippResponse
    }
}