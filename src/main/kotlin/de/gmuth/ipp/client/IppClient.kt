package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2021 Gerhard Muth
 */

import de.gmuth.http.Http
import de.gmuth.http.HttpURLConnectionClient
import de.gmuth.http.SSLHelper
import de.gmuth.ipp.core.*
import de.gmuth.log.Logging
import java.io.File
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

    fun exchangeSuccessful(ippRequest: IppRequest) =
            exchange(ippRequest).apply {
                if (!isSuccessful()) {
                    val statusMessage = operationGroup.getValueOrNull("status-message") ?: IppString("")
                    throw IppExchangeException(ippRequest, this, "${ippRequest.operation} failed: '$status' $statusMessage")
                }
            }

    fun exchange(ippRequest: IppRequest): IppResponse {
        val ippUri: URI = ippRequest.operationGroup.getValueOrNull("printer-uri") ?: throw IppException("missing 'printer-uri'")
        log.trace { "send '${ippRequest.operation}' request to $ippUri" }
        val httpUri = httpUri(ippUri)

        // http post binary ipp request
        val httpResponseStream = with(httpClient.post(
                httpUri,
                ippContentType,
                { httpPostStream -> ippRequest.write(httpPostStream) },
                httpBasicAuth
        )) {
            log.trace { "ipp-server: $server" }
            if (!isOK()) {
                if (status == 400) { // bad request
                    val badRequestFile = ippRequest.saveRawBytes(File("bad_ipp_request_${ippRequest.requestId}.bin"))
                    log.warn { "bad ipp request written to file ${badRequestFile.absolutePath}" }
                }
                throw IppExchangeException(
                        ippRequest, null, "http request to $httpUri failed: status=$status, content-type=$contentType${textContent()}"
                )
            }
            if (contentType == null) throw IppExchangeException(ippRequest, null, "missing content-type in http response")
            else if (!contentType.startsWith(ippContentType)) throw IppExchangeException(ippRequest, null, "invalid content-type: $contentType")
            else contentStream!!
        }

        return IppResponse().apply {
            // decode ipp response
            try {
                read(httpResponseStream)
                log.debug { "$ippUri: $ippRequest => $this" }
            } catch (exception: Exception) {
                throw IppExchangeException(ippRequest, this, "failed to decode ipp response", exception).apply {
                    saveRequestAndResponse("decoding_ipp_response_${ippRequest.requestId}_failed")
                }
            }
            // response logging
            if (operationGroup.containsKey("status-message")) log.debug { "status-message: $statusMessage" }
            if (containsGroup(IppTag.Unsupported)) unsupportedGroup.values.forEach { log.warn { "unsupported attribute: $it" } }
        }
    }

    // convert ipp uri to http uri
    internal fun httpUri(ippUri: URI): URI = with(ippUri) {
        val scheme = scheme.replace("ipp", "http")
        val port = if (port == -1) 631 else port
        URI.create("$scheme://$host:$port$path")
    }

}