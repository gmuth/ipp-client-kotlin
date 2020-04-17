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

class IppClient(
        var ippVersion: IppVersion = IppVersion(1, 1),
        val httpClient: Http.Client = HttpClientByHttpURLConnection()
        //val httpClient: Http.Client = HttpClientByJava11HttpClient()
) {
    companion object {
        private val requestCounter = AtomicInteger(1)
    }

    var verbose: Boolean = false

    fun exchangeSuccessful(
            uri: URI,
            request: IppRequest,
            exceptionMessage: String = "${request.operation} failed",
            documentInputStream: InputStream? = null,
            httpAuth: Http.Auth? = null

    ): IppResponse {
        val response = exchange(uri, request, documentInputStream, httpAuth)
        if (response.isSuccessful()) return response
        else throw IppExchangeException(request, response, "$exceptionMessage: '${response.status}' ${response.statusMessage ?: ""}")
    }

    fun exchange(
            uri: URI,
            request: IppRequest,
            documentInputStream: InputStream? = null,
            httpAuth: Http.Auth? = null

    ): IppResponse {
        // request logging
        with(request) {
            if (verbose) {
                println("send ${operation} request to $uri")
                println(this)
                logDetails(">> ")
            }
        }

        val requestInputStream = try {
            request.toInputStream()
        } catch (exception: Exception) {
            throw IppExchangeException(request, null, "failed to encode ipp request", exception)
        }
        val responseStream = exchange(uri, requestInputStream, documentInputStream, httpAuth)
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
                    for(attribute in unsupported.values) {
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
            documentInputStream: InputStream? = null,
            httpAuth: Http.Auth? = null

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
                    throw IppException("response from $uri is invalid: http-status = $status, content-type = ${content.type}$text")
                }
            }
        } catch (sslException: SSLHandshakeException) {
            println("WARN: set disableSSLCertificateValidation to true to accept self-signed certificates (e.g. with cups)")
            throw IppException("SSL connection error $httpUri", sslException)
        }
    }

    fun ippRequest(operation: IppOperation) =
            IppRequest(
                    ippVersion,
                    operation,
                    requestCounter.getAndIncrement()

            ).apply {
                operationGroup.attribute("requesting-user-name", IppTag.NameWithoutLanguage, System.getenv("USER"))
            }

    //-----------------------
    // Get-Printer-Attributes
    // ----------------------

    fun getPrinterAttributes(printerUri: URI, vararg requestedAttributes: String): IppResponse {
        return getPrinterAttributes(printerUri, requestedAttributes.toList())
    }

    fun getPrinterAttributes(printerUri: URI, requestedAttributes: List<String> = listOf()): IppResponse {
        val request = ippRequest(IppOperation.GetPrinterAttributes).apply {
            operationGroup.attribute("printer-uri", IppTag.Uri, printerUri)
            if (requestedAttributes.isNotEmpty()) {
                operationGroup.attribute("requested-attributes", IppTag.Keyword, requestedAttributes)
            }
        }
        return exchangeSuccessful(printerUri, request, "Get-Printer-Attributes $printerUri")
    }

    //-------------------
    // Get-Job-Attributes
    // ------------------

    fun getJobAttributes(printerUri: URI, jobId: Int): IppResponse {
        val request = ippRequest(IppOperation.GetJobAttributes).apply {
            operationGroup.attribute("printer-uri", IppTag.Uri, printerUri)
            operationGroup.attribute("job-id", IppTag.Integer, jobId)
        }
        return exchangeSuccessful(printerUri, request, "Get-Job-Attributes #$jobId failed")
    }

    fun getJobAttributes(jobUri: URI): IppResponse {
        val request = ippRequest(IppOperation.GetJobAttributes).apply {
            operationGroup.attribute("job-uri", IppTag.Uri, jobUri)
        }
        return exchangeSuccessful(jobUri, request, "Get-Job-Attributes $jobUri failed")
    }

}