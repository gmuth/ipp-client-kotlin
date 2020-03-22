package de.gmuth.ipp.core

/**
 * Author: Gerhard Muth
 */

import de.gmuth.http.HttpContent
import de.gmuth.http.HttpPostContent
import de.gmuth.http.HttpPostContentWithJava11HttpClient
import java.io.IOException
import java.io.InputStream
import java.io.SequenceInputStream
import java.net.URI
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicInteger

class IppClient(
        private val printerURI: URI,
        private val charset: Charset = Charsets.US_ASCII,
        private val naturalLanguage: String = "en",
        private val httpContentPoster: HttpPostContent = HttpPostContentWithJava11HttpClient()

) {
    private val requestCounter = AtomicInteger(1)

    fun exchangeIpp(ippRequest: IppRequest, documentInputStream: InputStream? = null): IppResponse {
        println("send ipp request (${ippRequest.getCodeDescription()}) to $printerURI and read ipp response")
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

        val ippRequestContent = HttpContent(
                ippContentType,
                if (documentInputStream == null) ippRequestStream
                else SequenceInputStream(ippRequestStream, documentInputStream)
        )

        val httpResponse = httpContentPoster.post(printerURI, ippRequestContent)
        with(httpResponse) {
            if (content.type.startsWith("text")) {
                // show what went wrong
                println(String(content.stream.readAllBytes()))
            }
            if (content.type != ippContentType) {
                throw IOException("response from $printerURI is not '$ippContentType'")
            }
            if (status != 200) {
                throw IOException("post to $printerURI failed with http status $status")
            }
            return content.stream
        }
    }

}