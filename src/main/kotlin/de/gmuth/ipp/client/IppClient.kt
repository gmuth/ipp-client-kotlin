package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.http.Http
import de.gmuth.http.HttpClientByHttpURLConnection
import de.gmuth.ipp.core.*
import java.io.InputStream
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.SSLHandshakeException

class IppClient(
        var ippVersion: String = "1.1",
        val httpClient: Http.Client = HttpClientByHttpURLConnection(), // HttpClientByJava11HttpClient()
        val requestingUserName: String? = System.getProperty("user.name")

) : IppExchange {

    private val requestCounter = AtomicInteger(1)
    var httpAuth: Http.Auth? = null
    var verbose: Boolean = false

    //-------------------------------------
    // factory/build methods for IppRequest
    //-------------------------------------

    fun ippRequest(operation: IppOperation, printerUri: URI? = null) =
            IppRequest(ippVersion, operation.code, requestCounter.getAndIncrement()).apply {
                if (printerUri != null) {
                    operationGroup.attribute("printer-uri", IppTag.Uri, printerUri)
                }
                if (requestingUserName != null) {
                    operationGroup.attribute("requesting-user-name", IppTag.NameWithoutLanguage, requestingUserName)
                }
            }

    fun ippJobRequest(jobOperation: IppOperation, printerUri: URI, jobId: Int) =
            ippRequest(jobOperation, printerUri).apply {
                operationGroup.attribute("job-id", IppTag.Integer, jobId)
            }

    //-------------------------------------------
    // exchange methods for IppRequest/IppRequest
    //-------------------------------------------

    fun exchangeSuccessful(ippUri: URI, ippRequest: IppRequest): IppResponse {
        val ippResponse = exchange(ippUri, ippRequest)
        if (ippResponse.isSuccessful()) {
            return ippResponse
        } else {
            val exceptionMessage = "'${ippRequest.operation}' failed: '${ippResponse.status}' ${ippResponse.statusMessage ?: ""}"
            throw IppExchangeException(ippRequest, ippResponse, exceptionMessage)
        }
    }

    override fun exchange(ippUri: URI, ippRequest: IppRequest): IppResponse {
        val ippResponse = IppResponse()
        // internal function
        fun logRequestResponseDetails() {
            ippRequest.logDetails("IPP-REQUEST: ")
            println("exchanged @ $ippUri")
            ippResponse.logDetails("IPP-RESPONSE: ")
        }
        // request logging
        if (verbose) {
            println("send '${ippRequest.operation}' request to $ippUri")
            ippRequest.logDetails(">> ")
        }
        // convert ipp uri to http uri
        val httpUri = with(ippUri) {
            val scheme = scheme.replace("ipp", "http")
            val port = if (port == -1) 631 else port
            URI.create("$scheme://$host:$port$path")
        }
        // http exchange binary ipp message
        val ippResponseStream = httpExchange(httpUri, ippRequest.inputStream)
        // decode ipp response
        try {
            ippResponse.decode(ippResponseStream)
        } catch (exception: Exception) {
            logRequestResponseDetails()
            throw IppExchangeException(ippRequest, ippResponse, "failed to decode ipp response", exception)
        }
        // response logging
        if (verbose) {
            println("exchanged @ $ippUri")
            ippResponse.logDetails("<< ")
            with(ippResponse.statusMessage) {
                if (this != null) println("status-message: $this")
            }
        }
        // failure logging
        if (!ippResponse.isSuccessful()) {
            logRequestResponseDetails()
        }
        // unsupported attributes
        for (unsupported in ippResponse.getAttributesGroups(IppTag.Unsupported)) {
            for (attribute in unsupported.values) {
                println("WARN: unsupported attribute: $attribute")
            }
        }
        // the decoded response
        return ippResponse
    }

    private fun httpExchange(httpUri: URI, httpContentStream: InputStream): InputStream {
        val contentType = "application/ipp"
        val httpRequestContent = Http.Content(contentType, httpContentStream)
        try {
            with(httpClient.post(httpUri, httpRequestContent, httpAuth)) {
                if (status == 200 && content.type == contentType) {
                    return content.stream

                } else {
                    val textContent =
                            if (content.type.startsWith("text")) {
                                ", content=" + String(content.stream.readAllBytes())
                            } else {
                                ""
                            }
                    when (status) {
                        426 -> println("ERROR: HTTP 426 suggests using a secure connection for authentication, try setting 'requesting-user-name'")
                        401 -> println("ERROR: HTTP 401 unauthorized, try setting 'requesting-user-name'")
                    }
                    throw IppException("http request to $httpUri failed: http-status=$status, content-type=${content.type}$textContent")
                }
            }
        } catch (sslException: SSLHandshakeException) {
            throw IppException("SSL connection error $httpUri", sslException)
        }
    }
}