package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.http.Http
import de.gmuth.http.HttpURLConnectionClient
import de.gmuth.http.SSLHelper
import de.gmuth.ipp.core.*
import de.gmuth.log.Logging
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.SSLHandshakeException

open class IppClient(
        var ippVersion: IppVersion = IppVersion(),
        val httpClient: Http.Client = HttpURLConnectionClient(),
        var httpBasicAuth: Http.BasicAuth? = null,
        val requestingUserName: String? = if (httpBasicAuth == null) System.getProperty("user.name") else httpBasicAuth.user
) {
    var logDetails: Boolean = false
    var requestCharset: Charset = Charsets.UTF_8
    var requestNaturalLanguage: String = "en"
    var lastIppRequest: IppRequest? = null
    var lastIppResponse: IppResponse? = null
    private val requestCounter = AtomicInteger(1)

    fun trustAnyCertificate() {
        httpClient.config.sslSocketFactory = SSLHelper.sslSocketFactoryForAnyCertificate()
    }

    fun basicAuth(user: String, password: String) {
        httpBasicAuth = Http.BasicAuth(user, password)
    }

    companion object {
        val log = Logging.getLogger(Logging.LogLevel.WARN) {}
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

    //-------------------------------------------
    // exchange methods for IppRequest/IppRequest
    //-------------------------------------------

    fun exchangeSuccessful(ippRequest: IppRequest) =
            with(exchange(ippRequest)) {
                if (!isSuccessful()) {
                    if (logDetails) logDetails("<< ")
                    val statusMessage = operationGroup.getValueOrNull("status-message") ?: IppString("")
                    val message = "operation ${ippRequest.operation} failed: '$status' $statusMessage"
                    throw IppExchangeException(ippRequest, this, message)
                }
                log.debug { this.toString() }
                this // successful ippResponse
            }

    fun exchange(ippRequest: IppRequest): IppResponse {
        val ippUri: URI = ippRequest.operationGroup.getValueOrNull("printer-uri") ?: throw IppException("missing 'printer-uri'")
        lastIppRequest = ippRequest

        // request logging
        log.debug { "send '${ippRequest.operation}' request to $ippUri" }
        if (logDetails) ippRequest.logDetails(">> ")

        // convert ipp uri to http uri
        val httpUri = with(ippUri) {
            val scheme = scheme.replace("ipp", "http")
            val port = if (port == -1) 631 else port
            URI.create("$scheme://$host:$port$path")
        }

        // http exchange binary ipp message
        val ippResponseStream = httpExchange(httpUri) { ippRequestStream -> ippRequest.write(ippRequestStream) }

        // decode ipp response
        val ippResponse = IppResponse()
        try {
            ippResponse.read(ippResponseStream)
        } catch (exception: Exception) {
            if (logDetails) ippResponse.logDetails("<< ")
            throw IppExchangeException(ippRequest, ippResponse, "failed to decode ipp response", exception).apply {
                saveRequestAndResponse("ipp_decoding_failed")
            }
        } finally {
            log.info { String.format("%-75s=> %s", ippRequest, ippResponse) }
        }

        // response logging
        log.debug { "exchanged @ $ippUri" }
        if (logDetails) ippResponse.logDetails("<< ")
        if (ippResponse.operationGroup.containsKey("status-message")) {
            log.debug { "status-message: ${ippResponse.statusMessage}" }
        }

        // unsupported attributes
        if (ippResponse.containsGroup(IppTag.Unsupported)) {
            ippResponse.unsupportedGroup.values.forEach {
                log.warn { "unsupported attribute: $it" }
            }
        }

        // the decoded response
        lastIppResponse = ippResponse
        return ippResponse
    }

    open fun httpExchange(uri: URI, writeContent: (OutputStream) -> Unit): InputStream {
        val start = System.currentTimeMillis()
        try {
            with(httpClient.post(uri, ippContentType, writeContent, httpBasicAuth)) {
                log.debug { "ipp-server: $server" }
                if (status == 200 && contentType!!.startsWith(ippContentType)) return contentStream!!
                if (status == 401) log.warn { "invalid credentials. call basicAuth(\"user\", \"password\") to set valid credentials." }
                val textContent = StringBuffer().apply {
                    if (contentStream != null && contentType != null && contentType.startsWith("text")) {
                        append(", content=" + contentStream.bufferedReader().use { it.readText() })
                    }
                }
                throw IppException("http request to $uri failed: http-status=$status, content-type=${contentType}$textContent")
            }
        } catch (sslException: SSLHandshakeException) {
            throw IppException("SSL connection error $uri", sslException)
        } finally {
            val duration = System.currentTimeMillis() - start
            if (duration > 5000) log.warn { String.format("http exchange %s: %d ms", uri, duration) }
        }
    }

}