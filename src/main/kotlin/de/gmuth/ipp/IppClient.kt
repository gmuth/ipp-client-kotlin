package de.gmuth.ipp

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

class IppClient(
        private val printerUri: URI,
        private val ippVersion: IppVersion = IppVersion(1, 1),
        private val httpContentPoster: HttpPostContent = HttpPostContentWithJava11HttpClient()
) {
    init {
        IppMessage.verbose = true
    }

    private fun exchangeIpp(uri: URI, ippRequestStream: InputStream, documentInputStream: InputStream? = null): InputStream {
        val ippContentType = "application/ipp"

        val ippRequestContent = HttpContent(
                ippContentType,
                if (documentInputStream == null) ippRequestStream
                else SequenceInputStream(ippRequestStream, documentInputStream)
        )
        val httpResponse = httpContentPoster.post(uri, ippRequestContent)

        with(httpResponse) {
            if (content.type.startsWith("text")) {
                // show what went wrong
                println(String(content.stream.readAllBytes()))
            }
            if (content.type != ippContentType) {
                throw IOException("response from $uri is not '$ippContentType'")
            }
            if (status != 200) {
                throw IOException("post to $uri failed with http status $status")
            }
            return content.stream
        }
    }

    private fun exchangeIpp(uri: URI, ippMessage: IppMessage, documentInputStream: InputStream? = null): IppMessage {
        println("send ipp request to $uri and read ipp response")
        ippMessage.version = ippVersion
        val ippResponseStream = exchangeIpp(uri, ippMessage.toInputStream(), documentInputStream)
        val ippResponse = IppMessage.ofInputStream(ippResponseStream)
        with(ippResponse) {
            println("status-code: $status")
            if (statusMessage != null) println("status-message: $statusMessage")
        }
        return ippResponse
    }

    /**
     * Print document
     */
    fun printDocument(documentInputStream: InputStream, documentFormat: String? = null) {
        val ippRequest = IppPrintJobOperation(printerUri, documentFormat)
        val ippResponse = exchangeIpp(printerUri, ippRequest, documentInputStream)
        if (!ippResponse.status.successfulOk()) {
            println("failed to print to $printerUri")
        }
    }

}