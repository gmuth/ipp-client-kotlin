package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.http.Http
import de.gmuth.http.HttpClientByHttpURLConnection
import de.gmuth.ipp.core.IppOperation
import de.gmuth.ipp.core.IppRequest
import de.gmuth.ipp.core.IppResponse
import java.io.IOException
import java.io.InputStream
import java.io.SequenceInputStream
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger

class IppClient(
        val printerUri: URI,
        private val httpClient: Http.Client = HttpClientByHttpURLConnection()
        //private val httpClient: Http.Client = HttpClientByJava11HttpClient()
) {
    private val requestCounter = AtomicInteger(1)

    fun exchangeIpp(ippRequest: IppRequest, documentInputStream: InputStream? = null): IppResponse {
        println("send ${ippRequest.operation} request to $printerUri")
        ippRequest.requestId = requestCounter.getAndIncrement()
        ippRequest.logDetails(">")
        val ippRequestStream = ippRequest.toInputStream()
        val ippResponseStream = exchangeIpp(ippRequestStream, documentInputStream)
        println("read ipp response")
        val ippResponse = IppResponse.fromInputStream(ippResponseStream)
        with(ippResponse) {
            logDetails("<")
            println("status-code = $status")
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

        val httpResponse = httpClient.post(printerUri.replaceIppScheme(), ippRequestContent)
        with(httpResponse) {
            if (status == 200 && content.type == ippContentType) {
                return content.stream

            } else {
                val text = if (content.type.startsWith("text")) ", content = " + String(content.stream.readAllBytes()) else ""
                throw IOException("response from $printerUri is invalid: http-status = $status, content-type = ${content.type}$text")
            }
        }
    }

    fun printDocument(inputStream: InputStream, documentFormat: String? = null): IppResponse {
        val ippRequest = IppRequest(IppOperation.PrintJob)
        ippRequest.addOperationAttribute("printer-uri", printerUri.toString())
        if (documentFormat != null) {
            ippRequest.addOperationAttribute("document-format", documentFormat)
        }
        val ippResponse = exchangeIpp(ippRequest, inputStream)
        if (!ippResponse.status.successfulOk()) {
            println("printing to $printerUri failed")
        }
        return ippResponse
    }
}

fun URI.replaceIppScheme(): URI {
    val httpScheme = scheme.replace("ipp", "http")
    return URI.create("${httpScheme}:${schemeSpecificPart}")
}