package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.http.Http
import de.gmuth.http.HttpByHttpURLConnection
import java.io.IOException
import java.io.InputStream
import java.io.SequenceInputStream
import java.net.URI
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicInteger

class IppClient(
        val printerUri: URI,
        private val charset: Charset = Charsets.US_ASCII,
        private val naturalLanguage: String = "en",
        private val httpClient: Http = HttpByHttpURLConnection()

) {
    private val requestCounter = AtomicInteger(1)

    fun exchangeIpp(ippRequest: IppRequest, documentInputStream: InputStream? = null): IppResponse {
        println("send ${ippRequest.operation} request to $printerUri")
        ippRequest.requestId = requestCounter.getAndIncrement()
        val ippRequestStream = ippRequest.toInputStream(charset, naturalLanguage)
        val ippResponseStream = exchangeIpp(ippRequestStream, documentInputStream)
        val ippResponse = IppResponse.ofInputStream(ippResponseStream)
        with(ippResponse) {
            if (!IppMessage.verbose) println("status-code = $status")
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

}

fun URI.replaceIppScheme(): URI {
    val httpScheme = scheme.replace("ipp", "http")
    return URI.create("${httpScheme}:${schemeSpecificPart}")
}