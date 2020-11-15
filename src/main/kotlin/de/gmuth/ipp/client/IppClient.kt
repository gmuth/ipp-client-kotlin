package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.http.Http
import de.gmuth.http.HttpURLConnectionClient
import de.gmuth.ipp.core.*
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.SSLHandshakeException

open class IppClient(
        var ippVersion: IppVersion = IppVersion(),
        val httpClient: Http.Client = HttpURLConnectionClient(),
        val requestingUserName: String? = System.getProperty("user.name")

) : IppExchange {
    var verbose: Boolean = false
    var requestCharset: Charset = Charsets.UTF_8
    var requestNaturalLanguage: String = "en"
    var httpBasicAuth: Http.BasicAuth? = null
    var lastIppRequest: IppRequest? = null
    var lastIppResponse: IppResponse? = null
    private val requestCounter = AtomicInteger(1)

    //------------------------------------
    // factory/build method for IppRequest
    //------------------------------------

    fun ippRequest(operation: IppOperation, printerUri: URI) = IppRequest(
            operation,
            printerUri,
            requestingUserName,
            ippVersion,
            requestCounter.getAndIncrement(),
            requestCharset,
            requestNaturalLanguage
    )

    //-------------------------------------------
    // exchange methods for IppRequest/IppRequest
    //-------------------------------------------

    fun exchangeSuccessful(ippRequest: IppRequest): IppResponse {
        val ippResponse = exchange(ippRequest)
        if (ippResponse.isSuccessful()) {
            if (verbose) println(ippResponse)
            return ippResponse
        } else {
            val exceptionMessage = "'${ippRequest.operation}' failed: '${ippResponse.status}' ${ippResponse.statusMessage ?: ""}"
            throw IppExchangeException(ippRequest, ippResponse, exceptionMessage)
        }
    }

    override fun exchange(ippRequest: IppRequest): IppResponse {
        val ippUri = ippRequest.printerUri
        lastIppRequest = ippRequest
        val ippResponse = IppResponse()
        // request logging
        if (verbose) {
            println("send '${ippRequest.operation}' request to $ippUri")
            ippRequest.logDetails(">> ")
        }
        // convert ipp uri to http uri
        val httpUri = with(ippUri) {
            val scheme = scheme.replace("ipp", "http")
            val port = if (port == -1) 631 else port
            URI.create("$scheme://$host:$port$path")
        }
        // http exchange binary ipp message
        val ippResponseStream = httpExchange(httpUri) { ippRequestStream -> ippRequest.write(ippRequestStream) }
        // decode ipp response
        try {
            ippResponse.read(ippResponseStream)
        } catch (exception: Exception) {
            if (ippResponse.rawBytes != null) {
                File("ipp_decoding_failed.response").writeBytes(ippResponse.rawBytes!!)
                println("WARN: ipp response written to file 'ipp_decoding_failed.response'")
            }
            throw IppExchangeException(ippRequest, ippResponse, "failed to decode ipp response", exception)
        } finally {
            println(String.format("%-75s=> %s", ippRequest, ippResponse))
        }
        // response logging
        if (verbose) {
            println("exchanged @ $ippUri")
            ippResponse.logDetails("<< ")
            with(ippResponse.statusMessage) {
                this?.let { println("status-message: $it") }
            }
        }
        // unsupported attributes
        for (unsupported in ippResponse.getAttributesGroups(IppTag.Unsupported)) {
            for (attribute in unsupported.values) {
                println("WARN: unsupported attribute: $attribute")
            }
        }
        // the decoded response
        lastIppResponse = ippResponse
        return ippResponse
    }

    open fun httpExchange(uri: URI, writeContent: (OutputStream) -> Unit): InputStream {
        val ippContentType = "application/ipp"
        val start = System.currentTimeMillis()
        try {
            with(httpClient.post(uri, ippContentType, writeContent, httpBasicAuth)) {
                if (status == 200 && contentType == ippContentType) return contentStream
                val textContent =
                        if (contentType.startsWith("text")) {
                            ", content=" + contentStream.bufferedReader().use { it.readText() }
                        } else {
                            ""
                        }
                throw IppException("http request to $uri failed: http-status=$status, content-type=${contentType}$textContent")
            }
        } catch (sslException: SSLHandshakeException) {
            throw IppException("SSL connection error $uri", sslException)
        } finally {
            val duration = System.currentTimeMillis() - start
            if (verbose || duration > 5000) println(String.format("http exchange %s: %d ms", uri, duration))
        }
    }

    fun writeLastIppRequest(file: File) {
        file.writeBytes(lastIppRequest!!.rawBytes ?: throw RuntimeException("missing raw bytes to write"))
    }

    fun writeLastIppResponse(file: File) {
        file.writeBytes(lastIppResponse!!.rawBytes ?: throw RuntimeException("missing raw bytes to write"))
    }

}