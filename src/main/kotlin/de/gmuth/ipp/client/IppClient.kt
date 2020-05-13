package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.http.Http
import de.gmuth.http.HttpClientByHttpURLConnection
import de.gmuth.ipp.core.*
import java.io.InputStream
import java.io.SequenceInputStream
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.SSLHandshakeException

open class IppClient(
        var ippVersion: IppVersion = IppVersion(1, 1),
        val httpClient: Http.Client = HttpClientByHttpURLConnection()
        //val httpClient: Http.Client = HttpClientByJava11HttpClient()
) {
    companion object {
        private val requestCounter = AtomicInteger(1)
    }

    var requestingUserName: String = System.getenv("USER")
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
                operationGroup.attribute("requesting-user-name", IppTag.NameWithoutLanguage, requestingUserName)
            }

    fun ippJobRequest(jobOperation: IppOperation, printerUri: URI, jobId: Int) =
            ippRequest(jobOperation, printerUri).apply {
                operationGroup.attribute("job-id", IppTag.Integer, jobId)
            }

    //-------------------------------------------s
    // exchange methods for IppRequest/IppRequest
    //-------------------------------------------

    fun exchangeSuccessful(
            uri: URI,
            request: IppRequest,
            documentInputStream: InputStream? = null

    ): IppResponse {
        val response = exchange(uri, request, documentInputStream)
        if (response.isSuccessful()) {
            return response
        } else {
            val exceptionMessage = "${request.codeDescription} failed: '${response.status}' ${response.statusMessage ?: ""}"
            throw IppExchangeException(request, response, exceptionMessage)
        }
    }

    fun exchange(
            uri: URI,
            request: IppRequest,
            documentInputStream: InputStream? = null

    ): IppResponse {
        // request logging
        with(request) {
            if (verbose) {
                println("send ${request.codeDescription} request to $uri")
                println(this)
                logDetails(">> ")
            }
        }

        val requestInputStream = try {
            request.toInputStream()
        } catch (exception: Exception) {
            throw IppExchangeException(request, null, "failed to encode ipp request", exception)
        }
        val responseStream = exchange(uri, requestInputStream, documentInputStream)
        val response = IppResponse()
        try {
            response.readFrom(responseStream)

            // response logging
            with(response) {
                if (verbose) {
                    println("read ipp response")
                    logDetails("<< ")
                    println(this)
                }
                if (!isSuccessful()) {
                    request.logDetails("IPP-REQUEST: ")
                    println("response from $uri")
                    logDetails("IPP-RESPONSE: ")
                }
                if (statusMessage != null) {
                    println("status-message: $statusMessage")
                }
                // warn about unsupported attributes
                for (unsupported in getAttributesGroups(IppTag.Unsupported)) {
                    for (attribute in unsupported.values) {
                        println("WARN: unsupported attribute: $attribute")
                    }
                }
            }
            return response

        } catch (exception: Exception) {
            request.logDetails("IPP-REQUEST: ")
            println("response from $uri")
            response.logDetails("IPP-RESPONSE: ")
            throw IppExchangeException(request, response, "failed to decode ipp response", exception)
        }
    }

    private fun exchange(
            uri: URI,
            requestStream: InputStream,
            documentInputStream: InputStream? = null

    ): InputStream {
        val contentType = "application/ipp"
        val requestContent = Http.Content(
                contentType,
                if (documentInputStream == null) requestStream
                else SequenceInputStream(requestStream, documentInputStream)
        )

        val httpUri = with(uri) {
            val scheme = scheme.replace("ipp", "http")
            val port = if (port == -1) 631 else port
            URI.create("$scheme://$host:$port$path")
        }

        try {
            // exchange http messages
            with(httpClient.post(httpUri, requestContent, httpAuth)) {
                if (status == 200 && content.type == contentType) {
                    return content.stream

                } else {
                    val text =
                            if (content.type.startsWith("text")) {
                                ", content = " + String(content.stream.readAllBytes())
                            } else {
                                ""
                            }
                    when (status) {
                        426 -> println("ERROR: HTTP 426 suggests using a secure connection for authentication, try setting 'requesting-user-name'")
                        401 -> println("ERROR: HTTP 401 unauthorized, try setting 'requesting-user-name'")
                    }
                    throw IppException("response from $uri is invalid: http-status = $status, content-type = ${content.type}$text")
                }
            }
        } catch (sslException: SSLHandshakeException) {
            throw IppException("SSL connection error $httpUri", sslException)
        }
    }
}