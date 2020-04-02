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
        val httpUri = with(uri) { URI.create("${scheme.replace("ipp", "http")}:${schemeSpecificPart}") }
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

    fun submitPrintJob(printerUri: URI, printJob: IppPrintJob, waitForTermination: Boolean = true): IppJob {
        val response = exchangeSuccessful(
                printerUri, printJob, "PrintJob failed", printJob.documentInputStream
        )
        val job = response.jobGroup.toIppJob()
        if (waitForTermination) {
            waitForTermination(job)
        }
        return job
    }

    private fun waitForTermination(job: IppJob, refreshRate: Duration = Duration.ofSeconds(1)) {
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

    // ---------------------------
    // Printer related operations
    // ---------------------------

}