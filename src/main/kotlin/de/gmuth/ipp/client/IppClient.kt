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
                ippRequest.logDetails("IPP-REQUEST: ")
                println("response from $printerUri")
                logDetails("IPP-RESPONSE: ")
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

    fun printFile(
            file: File,
            documentFormat: String? = "application/octet-stream",
            userName: String? = System.getenv("USER"),
            waitForTermination: Boolean = false

    ): IppJob {

        val ippRequest = IppRequest(IppOperation.PrintJob).apply {
            operationGroup.attribute("printer-uri", printerUri)
            operationGroup.attribute("document-format", documentFormat)
            operationGroup.attribute("requesting-user-name", userName)

            jobGroup.attribute("job-name", file.name)
            // CUPS extension: "output-mode" = color,monochrome,auto
            //jobGroup.attribute("output-mode", IppTag.Keyword, "monochrome")
        }

        val ippResponse = exchangeIpp(ippRequest, FileInputStream(file))
        if (!ippResponse.status.isSuccessful())
            throw IppException("PrintJob failed: $ippResponse")

        val ippJob = ippResponse.jobGroup.toIppJob()
        if (waitForTermination) waitForTermination(ippJob)
        return ippJob
    }

    // ----------------------
    // JOB related operations
    // ----------------------

    fun getJobAttributesById(printerUri: URI, id: Int): IppResponse {
        val ippRequest = IppRequest(IppOperation.GetJobAttributes).apply {
            operationGroup.attribute("printer-uri", printerUri)
            operationGroup.attribute("job-id", id)
        }
        with(exchangeIpp(ippRequest)) {
            if (status.isSuccessful()) return this
            else throw IppException("GetJobAttributes failed for job #$id: $status, $statusMessage")
        }
    }

    fun refreshJobAttributes(ippJob: IppJob) {
        val ippResponse = getJobAttributesById(printerUri, ippJob.id)
        ippJob.readFrom(ippResponse.jobGroup)
    }

    fun waitForTermination(ippJob: IppJob, refreshRate: Duration = Duration.ofSeconds(1)) {
        do {
            Thread.sleep(refreshRate.toMillis())
            refreshJobAttributes(ippJob)
            println("job-state = ${ippJob.state}")
        } while (!ippJob.state.isTerminated())
    }

}