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
import javax.net.ssl.SSLHandshakeException
import kotlin.streams.toList

class IppClient(
        var httpClient: Http.Client = HttpClientByHttpURLConnection()
        //val httpClient: Http.Client = HttpClientByJava11HttpClient()
) {
    var verbose: Boolean = false
    var auth: Http.Auth? = null

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

        try {
            with(httpClient.post(httpUri, requestContent, auth)) {
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

    fun exchangeSuccessful(
            uri: URI,
            request: IppRequest,
            exceptionMessage: String = "${request.operation} failed",
            documentInputStream: InputStream? = null

    ): IppResponse {
        val response = exchange(uri, request, documentInputStream)
        if (response.status.isSuccessful()) return response
        else throw IppExchangeException(request, response, "$exceptionMessage: '${response.status}' ${response.statusMessage ?: ""}")
    }

    // -------------------------
    // send PrintJob operation
    // -------------------------

    fun sendPrintJob(printJob: IppPrintJob, waitForTermination: Boolean = false): IppJob {
        val response = exchangeSuccessful(
                printJob.printerUri, printJob, "PrintJob failed", printJob.documentInputStream
        )
        val job = response.jobGroup.toIppJob()
        if (waitForTermination) {
            waitForTermination(job)
        }
        return job
    }

    // -------------------------------
    // Job related operations
    // -------------------------------

    fun getJobAttributes(printerUri: URI, jobId: Int): IppResponse {
        val request = IppRequest(IppOperation.GetJobAttributes).apply {
            operationGroup.attribute("printer-uri", IppTag.Uri, printerUri)
            operationGroup.attribute("job-id", IppTag.Integer, jobId)
        }
        return exchangeSuccessful(printerUri, request, "GetJobAttributes #$jobId failed")
    }

    fun getJobAttributes(jobUri: URI): IppResponse {
        val request = IppRequest(IppOperation.GetJobAttributes).apply {
            operationGroup.attribute("job-uri", IppTag.Uri, jobUri)
        }
        return exchangeSuccessful(jobUri, request, "GetJobAttributes $jobUri failed")
    }

    fun refreshJob(job: IppJob) {
        val response = getJobAttributes(job.uri)
        job.readFrom(response.jobGroup)
    }

    fun waitForTermination(job: IppJob, refreshRate: Duration = Duration.ofSeconds(1)) {
        do {
            Thread.sleep(refreshRate.toMillis())
            refreshJob(job)
            println("job-state = ${job.state}")
        } while (!job.state?.isTerminated()!!)
    }

    fun cancelJob(printerUri: URI, jobId: Int): IppResponse {
        val request = IppRequest(IppOperation.CancelJob, printerUri).apply {
            operationGroup.attribute("job-id", IppTag.Integer, jobId)
        }
        return exchangeSuccessful(printerUri, request)
    }

    fun cancelJob(job: IppJob): IppResponse {
        val request = IppRequest(IppOperation.CancelJob).apply {
            operationGroup.attribute("job-uri", IppTag.Uri, job.uri)
        }
        return exchangeSuccessful(job.uri, request)
    }

    fun getJob(printerUri: URI, jobId: Int) = getJobAttributes(printerUri, jobId).jobGroup.toIppJob()

    fun getJobs(printerUri: URI): List<IppJob> {
        val request = IppRequest(IppOperation.GetJobs, printerUri)
        val response = exchangeSuccessful(printerUri, request)
        return response.getAttributesGroups(IppTag.Job).stream()
                .map { jobGroup -> jobGroup.toIppJob() }.toList()
    }

    fun refreshJobs(jobs: List<IppJob>) = jobs.stream().forEach { job -> refreshJob(job) }

    // -----------------------------------------------
    // RFC 8011 4.2.2: optional operation to print uri
    // -----------------------------------------------

    fun printUri(
            printerUri: URI,
            documentUri: URI,
            documentFormat: String = "application/octet-stream",
            jobName: String = documentUri.path,
            waitForTermination: Boolean = false

    ): IppJob {

        val request = IppRequest(IppOperation.PrintUri, printerUri).apply {
            operationGroup.attribute("document-uri", IppTag.Uri, documentUri)
            operationGroup.attribute("document-format", IppTag.MimeMediaType, documentFormat)
            jobGroup.attribute("job-name", IppTag.NameWithoutLanguage, jobName)
        }
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

    fun pausePrinter(printerUri: URI) = sendPrinterOperation(printerUri, IppOperation.PausePrinter)
    fun resumePrinter(printerUri: URI) = sendPrinterOperation(printerUri, IppOperation.ResumePrinter)

    private fun sendPrinterOperation(printerUri: URI, printerOperation: IppOperation): IppResponse {
        if (auth == null) throw IllegalStateException("auth required")
        val request = IppRequest(printerOperation, printerUri)
        return exchangeSuccessful(printerUri, request)
    }

}