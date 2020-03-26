package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.http.Http
import de.gmuth.http.HttpClientByHttpURLConnection
import de.gmuth.ipp.core.IppJob
import de.gmuth.ipp.core.IppOperation
import de.gmuth.ipp.core.IppRequest
import de.gmuth.ipp.core.IppResponse
import java.io.IOException
import java.io.InputStream
import java.io.SequenceInputStream
import java.net.URI

class IppClient(
        val printerUri: URI,
        private val httpClient: Http.Client = HttpClientByHttpURLConnection()
        //private val httpClient: Http.Client = HttpClientByJava11HttpClient()
) {
    var verbose: Boolean = true

    fun exchangeIpp(ippRequest: IppRequest, documentInputStream: InputStream? = null): IppResponse {

        println("send ${ippRequest.operation} request to $printerUri")
        println(ippRequest)
        if (verbose) ippRequest.logDetails(">")
        val ippRequestStream = ippRequest.toInputStream()
        val ippResponseStream = exchangeIpp(ippRequestStream, documentInputStream)

        println("read ipp response")
        val ippResponse = IppResponse.fromInputStream(ippResponseStream)
        with(ippResponse) {
            if (verbose) logDetails("<")
            println(ippResponse)
            if (statusMessage != null) println("status-message: $statusMessage")
        }
        return ippResponse
    }

    private fun exchangeIpp(ippRequestStream: InputStream, documentInputStream: InputStream? = null): InputStream {
        val ippContentType = "application/ipp"

        val ippRequestContent = Http.Content(
                ippContentType,
                if (documentInputStream == null) ippRequestStream
                else SequenceInputStream(ippRequestStream, documentInputStream)
        )
        val httpUri = with(printerUri) { URI.create("${scheme.replace("ipp", "http")}:${schemeSpecificPart}") }
        val httpResponse = httpClient.post(httpUri, ippRequestContent)
        with(httpResponse) {
            if (status == 200 && content.type == ippContentType) {
                return content.stream

            } else {
                val text = if (content.type.startsWith("text")) ", content = " + String(content.stream.readAllBytes()) else ""
                throw IOException("response from $printerUri is invalid: http-status = $status, content-type = ${content.type}$text")
            }
        }
    }

    fun printDocument(
            inputStream: InputStream,
            documentFormat: String? = "application/octet-stream",
            userName: String? = "ipp-client-kotlin"

    ): IppResponse {

        val ippRequest = IppRequest(IppOperation.PrintJob).apply {
            addOperationAttribute("printer-uri", "$printerUri")
            addOperationAttribute("document-format", documentFormat)
            addOperationAttribute("requesting-user-name", userName)
        }

        val ippResponse = exchangeIpp(ippRequest, inputStream)
        if (ippResponse.status.successfulOk()) {

            val ippJobGroup = ippResponse.getSingleJobGroup()
            val ippJob = IppJob.fromIppAttributesGroup(ippJobGroup)
            println(ippJob)

        } else {
            println("printing to $printerUri failed")
        }
        return ippResponse
    }
}