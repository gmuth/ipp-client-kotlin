package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.http.Http
import de.gmuth.http.HttpClientByHttpURLConnection
import de.gmuth.ipp.core.*
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.SequenceInputStream
import java.net.URI
import java.time.Duration

class IppClient(
        private val httpClient: Http.Client = HttpClientByHttpURLConnection()
        //private val httpClient: Http.Client = HttpClientByJava11HttpClient()
) {
    var verbose: Boolean = false

    fun exchangeIpp(uri: URI, ippRequest: IppRequest, documentInputStream: InputStream? = null): IppResponse {
        val ippResponseStream = with(ippRequest) {
            if (verbose) {
                println("send ${operation} request to $uri")
                println(this)
                logDetails(">> ")
            }
            exchangeIpp(uri, toInputStream(), documentInputStream)
        }
        with(IppResponse.fromInputStream(ippResponseStream)) {
            if (verbose) {
                println("read ipp response")
                logDetails("<< ")
                println(this)
            }
            if (!status.isSuccessful()) {
                ippRequest.logDetails("IPP-REQUEST: ")
                println("response from $uri")
                logDetails("IPP-RESPONSE: ")
            }
            if (statusMessage != null) {
                println("status-message: $statusMessage")
            }
            return this
        }
    }

    private fun exchangeIpp(uri: URI, ippRequestStream: InputStream, documentInputStream: InputStream? = null): InputStream {
        val ippContentType = "application/ipp"
        val ippRequestContent = Http.Content(
                ippContentType,
                if (documentInputStream == null) ippRequestStream
                else SequenceInputStream(ippRequestStream, documentInputStream)
        )
        val httpUri = with(uri) { URI.create("${scheme.replace("ipp", "http")}:${schemeSpecificPart}") }
        with(httpClient.post(httpUri, ippRequestContent)) {
            if (status == 200 && content.type == ippContentType) {
                return content.stream

            } else {
                val text = if (content.type.startsWith("text")) ", content = " + String(content.stream.readAllBytes()) else ""
                throw IppException("response from $uri is invalid: http-status = $status, content-type = ${content.type}$text")
            }
        }
    }

    fun exchangeIppSuccessful(uri: URI, ippRequest: IppRequest, exceptionMessage: String, documentInputStream: InputStream? = null): IppResponse {
        val ippResponse = exchangeIpp(uri, ippRequest, documentInputStream)
        if (ippResponse.status.isSuccessful()) return ippResponse
        else throw IppException("$exceptionMessage: '${ippResponse.status}' ${ippResponse.statusMessage}")
    }

    // ---------------------------
    // Printer related operations
    // ---------------------------


    // ----------------------
    // JOB related operations
    // ----------------------

    fun printFile(
            printerUri: URI,
            file: File,
            documentFormat: String? = "application/octet-stream",
            waitForTermination: Boolean = false

    ): IppJob {

        val ippRequest = IppRequest(IppOperation.PrintJob).apply {
            operationGroup.attribute("printer-uri", printerUri)
            operationGroup.attribute("document-format", documentFormat)

            jobGroup.attribute("job-name", file.name)
            // CUPS extension: "output-mode" = color,monochrome,auto
            //jobGroup.attribute("output-mode", IppTag.Keyword, "monochrome")
        }
        val ippResponse = exchangeIppSuccessful(
                printerUri, ippRequest, "PrintJob '$file' failed", FileInputStream(file)
        )
        var ippJob = ippResponse.jobGroup.toIppJob()
        if (waitForTermination) {
            waitForTermination(ippJob)
        }
        return ippJob
    }

    fun waitForTermination(ippJob: IppJob, refreshRate: Duration = Duration.ofSeconds(1)) {
        do {
            Thread.sleep(refreshRate.toMillis())
            refreshJobAttributes(ippJob)
            println("job-state = ${ippJob.state}")
        } while (!ippJob.state.isTerminated())
    }

    fun refreshJobAttributes(ippJob: IppJob) {
        val ippResponse = getJobAttributes(ippJob.uri)
        ippJob.readFrom(ippResponse.jobGroup)
    }

    fun getJobAttributes(jobUri: URI): IppResponse {
        val ippRequest = IppRequest(IppOperation.GetJobAttributes).apply {
            operationGroup.attribute("job-uri", jobUri)
        }
        return exchangeIppSuccessful(jobUri, ippRequest, "GetJobAttributes failed $jobUri")
    }

    fun getJobAttributes(printerUri: URI, id: Int): IppResponse {
        val ippRequest = IppRequest(IppOperation.GetJobAttributes).apply {
            operationGroup.attribute("printer-uri", printerUri)
            operationGroup.attribute("job-id", id)
        }
        return exchangeIppSuccessful(printerUri, ippRequest, "GetJobAttributes failed for #$id")
    }
}