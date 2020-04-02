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
import java.time.Duration

class IppClient(
        private val httpClient: Http.Client = HttpClientByHttpURLConnection()
        //private val httpClient: Http.Client = HttpClientByJava11HttpClient()
) {
    var verbose: Boolean = false

    fun exchange(uri: URI, request: IppRequest, documentInputStream: InputStream? = null): IppResponse {
        val responseStream = with(request) {
            if (verbose) {
                println("send ${operation} request to $uri")
                println(this)
                logDetails(">> ")
            }
            exchange(uri, toInputStream(), documentInputStream)
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

    private fun exchange(uri: URI, requestStream: InputStream, documentInputStream: InputStream? = null): InputStream {
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

        with(httpClient.post(httpUri, requestContent)) {
            if (status == 200 && content.type == contentType) {
                return content.stream

            } else {
                val text = if (content.type.startsWith("text")) ", content = " + String(content.stream.readAllBytes()) else ""
                throw IppException("response from $uri is invalid: http-status = $status, content-type = ${content.type}$text")
            }
        }
    }

    fun exchangeSuccessful(uri: URI, request: IppRequest, exceptionMessage: String, documentInputStream: InputStream? = null): IppResponse {
        val response = exchange(uri, request, documentInputStream)
        if (response.status.isSuccessful()) return response
        else throw IppExchangeException(request, response, "$exceptionMessage: '${response.status}' ${response.statusMessage}")
    }

    // ----------------------
    // JOB related operations
    // ----------------------

    fun submitPrintJob(printerUri: URI, printJob: IppPrintJob, waitForTermination: Boolean = false): IppJob {
        val response = exchangeSuccessful(
                printerUri, printJob, "PrintJob failed", printJob.documentInputStream
        )

        val job = response.jobGroup.toIppJob()
        if (waitForTermination) {
            waitForTermination(job)
        }
        return job
    }

    fun waitForTermination(job: IppJob, refreshRate: Duration = Duration.ofSeconds(1)) {
        do {
            Thread.sleep(refreshRate.toMillis())
            refreshJobAttributes(job)
            println("job-state = ${job.state}")
        } while (!job.state.isTerminated())
    }

    private fun refreshJobAttributes(job: IppJob) {
        val ippRequest = IppGetJobAttributes(job.uri)
        val ippResponse = exchangeSuccessful(job.uri, ippRequest, "GetJobAttributes failed $job.uri")
        job.readFrom(ippResponse.jobGroup)
    }

    // -----------------------------------------------
    // RFC 8011 4.2.2: optional operation to print uri
    // -----------------------------------------------

    fun printUri(printerUri: URI, documentUri: URI, waitForTermination: Boolean = false): IppJob {
        val request = IppPrintUri(printerUri, documentUri)
        request.logDetails("IPP: ")

        val response = exchangeSuccessful(printerUri, request, "Print-Uri $documentUri")

        val job = response.jobGroup.toIppJob()
        if (waitForTermination) {
            waitForTermination(job)
        }
        return job
    }

    // ---------------------------
    // Printer related operations
    // ---------------------------

}