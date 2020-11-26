package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.http.Http
import de.gmuth.http.HttpURLConnectionClient
import de.gmuth.http.SSLHelper
import de.gmuth.ipp.core.*
import de.gmuth.log.Log
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.SSLHandshakeException

open class IppClient(
        var ippVersion: IppVersion = IppVersion(),
        val httpClient: Http.Client = HttpURLConnectionClient(),
        val requestingUserName: String? = System.getProperty("user.name")

) : IppExchange {
    var logDetails: Boolean = false
    var requestCharset: Charset = Charsets.UTF_8
    var requestNaturalLanguage: String = "en"
    var httpBasicAuth: Http.BasicAuth? = null
    var lastIppRequest: IppRequest? = null
    var lastIppResponse: IppResponse? = null
    private val requestCounter = AtomicInteger(1)

    fun trustAnyCertificate() {
        httpClient.config.sslSocketFactory = SSLHelper.sslSocketFactoryForAnyCertificate()
    }

    fun basicAuth(user: String = requestingUserName!!, password: String) {
        httpBasicAuth = Http.BasicAuth(user, password)
    }

    companion object {
        val log = Log.getWriter("IppClient")
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
                    val message = "operation ${ippRequest.operation} failed: '$status' $statusMessage"
                    throw IppExchangeException(ippRequest, this, message).apply { logDetails() }
                }
                log.debug { this.toString() }
                this // successful ippResponse
            }

    override fun exchange(ippRequest: IppRequest): IppResponse {
        val ippUri: URI = ippRequest.operationGroup.getValue("printer-uri") ?: throw IppException("missing printer-uri")
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
            if (ippResponse.rawBytes != null) {
                File("ipp_decoding_failed.response").writeBytes(ippResponse.rawBytes!!)
                log.warn { "ipp response written to file 'ipp_decoding_failed.response'" }
            }
            throw IppExchangeException(ippRequest, ippResponse, "failed to decode ipp response", exception)
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
        for (unsupported in ippResponse.getAttributesGroups(IppTag.Unsupported)) {
            for (attribute in unsupported.values) {
                log.warn { "unsupported attribute: $attribute" }
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
                if (status == 200 && contentType == ippContentType) return contentStream
                if (server != null && server.toLowerCase().contains("cups")) {
                    when (status) {
                        426 -> {
                            val httpsUri = with(uri) { URI.create("https://$host:$port$path") }
                            log.warn { "$server says '426 Upgrade Required' -> trying $httpsUri" }
                            return httpExchange(httpsUri, writeContent)
                        }
                        401 -> {
                            log.warn { "call basicAuth(\"user\", \"password\") to set credentials." }
                            throw IppException("$server says '401 Unauthorized'")
                        }
                    }
                }
                val textContent = StringBuffer().apply {
                    if (contentType.startsWith("text")) {
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

    fun writeLastIppRequest(file: File) {
        file.writeBytes(lastIppRequest!!.rawBytes ?: throw RuntimeException("missing raw bytes to write"))
    }

    fun writeLastIppResponse(file: File) {
        file.writeBytes(lastIppResponse!!.rawBytes ?: throw RuntimeException("missing raw bytes to write"))
    }

}