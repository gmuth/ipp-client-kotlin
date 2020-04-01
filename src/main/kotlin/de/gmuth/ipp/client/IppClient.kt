package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.http.Http
import de.gmuth.http.HttpClientByHttpURLConnection
import de.gmuth.ipp.core.*
import java.io.IOException
import java.io.InputStream
import java.io.SequenceInputStream
import java.net.URI
import java.time.Duration

class IppClient(
        val printerUri: URI,
        private val httpClient: Http.Client = HttpClientByHttpURLConnection()
        //private val httpClient: Http.Client = HttpClientByJava11HttpClient()
) {
    var verbose: Boolean = true

    fun exchangeIpp(ippRequest: IppRequest, documentInputStream: InputStream? = null): IppResponse {
        val ippResponseStream = with(ippRequest) {
            if (verbose) {
                println("send ${operation} request to $printerUri")
                println(this)
                logDetails(">> ")
            }
            exchangeIpp(toInputStream(), documentInputStream)
        }
        with(IppResponse.fromInputStream(ippResponseStream)) {
            if (verbose) {
                println("read ipp response")
                logDetails("<< ")
                println(this)
            }
            if (statusMessage != null) println("status-message: $statusMessage")
            if (!status.isSuccessful()) {
                println("WARN: request failed: $ippRequest")
                logDetails("WARN: ")
            }
            return this
        }
    }

    private fun exchangeIpp(ippRequestStream: InputStream, documentInputStream: InputStream? = null): InputStream {
        val ippContentType = "application/ipp"
        val ippRequestContent = Http.Content(
                ippContentType,
                if (documentInputStream == null) ippRequestStream
                else SequenceInputStream(ippRequestStream, documentInputStream)
        )
        val httpUri = with(printerUri) { URI.create("${scheme.replace("ipp", "http")}:${schemeSpecificPart}") }
        with(httpClient.post(httpUri, ippRequestContent)) {
            if (status == 200 && content.type == ippContentType) {
                return content.stream

            } else {
                val text = if (content.type.startsWith("text")) ", content = " + String(content.stream.readAllBytes()) else ""
                throw IppException("response from $printerUri is invalid: http-status = $status, content-type = ${content.type}$text")
            }
        }
    }

    // ---------------------------
    // DOCUMENT related operations
    // ---------------------------

    fun printDocument(
            inputStream: InputStream,
            documentFormat: String? = "application/octet-stream",
            userName: String? = "ipp-client-kotlin"

    ): IppJob {

        val ippRequest = IppRequest(IppOperation.PrintJob).apply {
            operationGroup.attribute("printer-uri", printerUri)
            operationGroup.attribute("document-format", documentFormat)
            operationGroup.attribute("requesting-user-name", userName)

            // PWG 5100.13: "print-color-mode"
            // CUPS extension: "output-mode" = color,monochrome,auto
            job.attribute("output-mode", IppTag.Keyword, "monochrome")
        }

        val ippResponse = exchangeIpp(ippRequest, inputStream)
        if (ippResponse.status.isSuccessful()) {
            val ippJobGroup = ippResponse.job
            val ippJob = ippJobGroup.toIppJob()
            println(ippJob)
            return ippJob

        } else {
            throw RuntimeException("printing to $printerUri failed")
        }
    }

    // ----------------------
    // JOB related operations
    // ----------------------

    fun getJobAttributes(id: Int): IppResponse {
        val ippRequest = IppRequest(IppOperation.GetJobAttributes).apply {
            operationGroup.attribute("printer-uri", printerUri)
            operationGroup.attribute("job-id", id)
        }
        return exchangeIpp(ippRequest)
    }

    fun updateJobAttributes(ippJob: IppJob) {
        val ippResponse = getJobAttributes(ippJob.id)
        if (ippResponse.status.isSuccessful()) {
            ippJob.readFrom(ippResponse.job)
        } else {
            println(ippResponse)
            throw IppException("updating job attributes failed")
        }
    }

    fun waitForTermination(ippJob: IppJob, pollInterval: Duration = Duration.ofSeconds(1)) {
        do {
            Thread.sleep(pollInterval.toMillis())
            updateJobAttributes(ippJob)
            println("job-state = ${ippJob.state}")
        } while (!ippJob.state.isTerminated())
    }

}