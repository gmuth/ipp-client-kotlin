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

    var requestingUserName: String? = System.getenv("USER")
    var verbose: Boolean = false

    fun exchangeSuccessful(
            uri: URI,
            request: IppRequest,
            exceptionMessage: String = "${request.operation} failed",
            documentInputStream: InputStream? = null,
            httpAuth: Http.Auth? = null

    ): IppResponse {
        val response = exchange(uri, request, documentInputStream, httpAuth)
        if (response.isSuccessful()) {
            return response
        } else {
            throw IppExchangeException(request, response, "$exceptionMessage: '${response.status}' ${response.statusMessage ?: ""}")
        }
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

    // ---- factory method for IppRequest

    fun ippRequest(operation: IppOperation, printerUri: URI? = null) =
            IppRequest(ippVersion, operation, requestCounter.getAndIncrement()).apply {
                if (printerUri != null) {
                    operationGroup.attribute("printer-uri", IppTag.Uri, printerUri)
                }
                if (requestingUserName != null) {
                    operationGroup.attribute("requesting-user-name", IppTag.NameWithoutLanguage, requestingUserName)
                }
            }

    //-----------------------
    // Get-Printer-Attributes
    //-----------------------

    fun getPrinterAttributes(printerUri: URI, requestedAttributes: List<String> = listOf()): IppResponse {
        val request = ippRequest(IppOperation.GetPrinterAttributes, printerUri).apply {
            if (requestedAttributes.isNotEmpty()) {
                operationGroup.attribute("requested-attributes", IppTag.Keyword, requestedAttributes)
            }
        }
        return exchangeSuccessful(printerUri, request, "Get-Printer-Attributes $printerUri")
    }

    //-------------------
    // Get-Job-Attributes
    //-------------------

    fun getJobAttributes(printerUri: URI, jobId: Int): IppResponse {
        val request = ippRequest(IppOperation.GetJobAttributes, printerUri).apply {
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

    //-----------
    // Cancel-Job
    //-----------

    fun cancelJob(printerUri: URI, jobId: Int): IppResponse {
        val request = ippRequest(IppOperation.CancelJob, printerUri).apply {
            operationGroup.attribute("job-id", IppTag.Integer, jobId)
        }
        return exchangeSuccessful(printerUri, request, "Cancel-Job #$jobId failed")
    }

    fun cancelJob(jobUri: URI): IppResponse {
        val request = ippRequest(IppOperation.CancelJob).apply {
            operationGroup.attribute("job-uri", IppTag.Uri, jobUri)
        }
        return exchangeSuccessful(jobUri, request, "Cancel-Job $jobUri failed")
    }

    //---------
    // Get-Jobs
    //---------

    // which-jobs-supported (1setOf keyword) = completed,not-completed,aborted,all,canceled,pending,pending-held,processing,processing-stopped

    fun getJobs(printerUri: URI, whichJobs: String? = null): IppResponse {
        val request = ippRequest(IppOperation.GetJobs, printerUri)
        if (whichJobs != null) {
            request.operationGroup.attribute("which-jobs", IppTag.Keyword, whichJobs)
        }
        return exchangeSuccessful(printerUri, request)
    }

    //-----------------
    // Identify-Printer
    //-----------------

    fun identifyPrinter(printerUri: URI, action: String, httpAuth: Http.Auth? = null) {
        val request = ippRequest(IppOperation.IdentifyPrinter, printerUri).apply {
            operationGroup.attribute("identify-actions", IppTag.Keyword, action)
        }
        exchangeSuccessful(printerUri, request, httpAuth = httpAuth)
    }

    //--------------
    // Pause-Printer
    //--------------

    fun pausePrinter(printerUri: URI, httpAuth: Http.Auth? = null) {
        val request = ippRequest(IppOperation.PausePrinter, printerUri)
        exchangeSuccessful(printerUri, request, httpAuth = httpAuth)
    }

    //---------------
    // Resume-Printer
    //---------------

    fun resumePrinter(printerUri: URI, httpAuth: Http.Auth? = null) {
        val request = ippRequest(IppOperation.ResumePrinter, printerUri)
        exchangeSuccessful(printerUri, request, httpAuth = httpAuth)
    }

}