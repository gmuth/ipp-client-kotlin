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
import javax.net.ssl.SSLHandshakeException

class IppClient(
        var httpClient: Http.Client = HttpClientByHttpURLConnection()
        //val httpClient: Http.Client = HttpClientByJava11HttpClient()
) {
    var verbose: Boolean = false

    fun exchangeSuccessful(
            uri: URI,
            request: IppRequest,
            exceptionMessage: String = "${request.operation} failed",
            documentInputStream: InputStream? = null,
            httpAuth: Http.Auth? = null

    ): IppResponse {
        val response = exchange(uri, request, documentInputStream, httpAuth)
        if (response.status.isSuccessful()) return response
        else throw IppExchangeException(request, response, "$exceptionMessage: '${response.status}' ${response.statusMessage ?: ""}")
    }

    fun exchange(
            uri: URI,
            request: IppRequest,
            documentInputStream: InputStream? = null,
            httpAuth: Http.Auth? = null

    ): IppResponse {
        val responseStream = with(request) {
            if (verbose) {
                println("send ${operation} request to $uri")
                println(this)
                logDetails(">> ")
            }
            exchange(uri, toInputStream(), documentInputStream, httpAuth)
        }
        with(IppResponse.fromInputStream(responseStream)) {
            if (verbose) {
                println("read ipp response")
                logDetails("<< ")
                println(this)
            }
            if (!status.isSuccessful()) {
                request.logDetails("IPP-REQUEST: ")
                println("response from $uri")
                logDetails("IPP-RESPONSE: ")
            }
            if (statusMessage != null) {
                println("status-message: $statusMessage")
            }
            return this
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
                    val text = if (content.type.startsWith("text")) ", content = " + String(content.stream.readAllBytes()) else ""
                    throw IppException("response from $uri is invalid: http-status = $status, content-type = ${content.type}$text")
                }
            }
        } catch (sslException: SSLHandshakeException) {
            println("WARN: set disableSSLCertificateValidation to true to accept self-signed certificates (e.g. with cups)")
            throw IppException("SSL connection error $httpUri", sslException)
        }
    }

    //-----------------------
    // Get-Printer-Attributes
    // ----------------------

    fun getPrinterAttributes(printerUri: URI, vararg requestedAttributes: String): IppResponse {
        return getPrinterAttributes(printerUri, requestedAttributes.toList())
    }

    fun getPrinterAttributes(printerUri: URI, requestedAttributes: List<String>): IppResponse {
        val request = IppRequest(IppOperation.GetPrinterAttributes, printerUri).apply {
            if (requestedAttributes.isNotEmpty())
                operationGroup.attribute("requested-attributes", IppTag.Keyword, requestedAttributes)
        }
        request.logDetails()
        return exchangeSuccessful(printerUri, request, "Get-Printer-Attributes $printerUri")
    }

    //-------------------
    // Get-Job-Attributes
    // ------------------

    fun getJobAttributes(printerUri: URI, jobId: Int): IppResponse {
        val request = IppRequest(IppOperation.GetJobAttributes).apply {
            operationGroup.attribute("printer-uri", IppTag.Uri, printerUri)
            operationGroup.attribute("job-id", IppTag.Integer, jobId)
        }
        return exchangeSuccessful(printerUri, request, "Get-Job-Attributes #$jobId failed")
    }

    fun getJobAttributes(jobUri: URI): IppResponse {
        val request = IppRequest(IppOperation.GetJobAttributes).apply {
            operationGroup.attribute("job-uri", IppTag.Uri, jobUri)
        }
        return exchangeSuccessful(jobUri, request, "Get-Job-Attributes $jobUri failed")
    }

}