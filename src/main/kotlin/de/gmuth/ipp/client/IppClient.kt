package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.client.IppExchangeException.ClientErrorNotFoundException
import de.gmuth.ipp.core.IppOperation
import de.gmuth.ipp.core.IppRequest
import de.gmuth.ipp.core.IppResponse
import de.gmuth.ipp.core.IppStatus.ClientErrorBadRequest
import de.gmuth.ipp.core.IppStatus.ClientErrorNotFound
import de.gmuth.ipp.core.IppTag.Unsupported
import de.gmuth.ipp.iana.IppRegistrationsSection2
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level.SEVERE
import java.util.logging.Logger.getLogger
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection

typealias IppResponseInterceptor = (request: IppRequest, response: IppResponse) -> Unit

open class IppClient(val config: IppConfig = IppConfig()) {
    protected val log = getLogger(javaClass.name)

    var responseInterceptor: IppResponseInterceptor? = null
    var saveMessages: Boolean = false
    var saveMessagesDirectory = File("ipp-messages")
    var onExceptionSaveMessages: Boolean = false
    var throwWhenNotSuccessful: Boolean = true

    fun basicAuth(user: String, password: String) {
        config.userName = user
        config.password = password
    }

    companion object {
        const val APPLICATION_IPP = "application/ipp"
    }

    //-----------------
    // build IppRequest
    //-----------------

    protected val requestCounter = AtomicInteger(1)

    fun ippRequest(
        operation: IppOperation,
        printerUri: URI,
        jobId: Int? = null,
        requestedAttributes: Collection<String>? = null,
        userName: String? = config.userName
    ) = IppRequest(
        operation,
        printerUri,
        jobId,
        requestedAttributes,
        userName,
        config.ippVersion,
        requestCounter.getAndIncrement(),
        config.charset,
        config.naturalLanguage
    )

    //------------------------------------
    // Exchange IppRequest for IppResponse
    //------------------------------------

    fun exchange(request: IppRequest): IppResponse {
        val ippUri: URI = request.printerUri
        log.finer { "send '${request.operation}' request to $ippUri" }
        val response = httpPost(toHttpUri(ippUri), request)
        log.fine { "$ippUri: $request => $response" }
        if (saveMessages) {
            fun file(suffix: String) = File(saveMessagesDirectory, "${request.requestId}-${request.operation}.$suffix")
            request.saveBytes(file("request"))
            response.saveBytes(file("response"))
            response.saveText(file("txt"))
        }
        responseInterceptor?.invoke(request, response)
        validateResponse(request, response)
        return response
    }

    //----------------------------------------------
    // HTTP post IPP request and decode IPP response
    //----------------------------------------------

    open fun httpPost(httpUri: URI, request: IppRequest): IppResponse {
        with(httpUri.toURL().openConnection() as HttpURLConnection) {
            if (this is HttpsURLConnection && config.sslContext != null) {
                sslSocketFactory = config.sslContext!!.socketFactory
                if (!config.verifySSLHostname) hostnameVerifier = HostnameVerifier { _, _ -> true }
            }
            configure(chunked = request.hasDocument())
            request.write(outputStream)
            val responseContentStream = try {
                validateResponse(request, inputStream)
                inputStream
            } catch (ioException: IOException) {
                validateResponse(request, errorStream, ioException)
                errorStream
            }
            return decodeContentStream(request, responseCode, responseContentStream)

        }
    }

    protected fun validateResponse(request: IppRequest, response: IppResponse) {
        with(response) {
            if (status == ClientErrorBadRequest) request.log(log, SEVERE, prefix = "BAD-REQUEST: ")
            if (containsGroup(Unsupported)) unsupportedGroup.values.forEach { log.warning() { "unsupported: $it" } }
            if (!isSuccessful()) {
                IppRegistrationsSection2.validate(request)
                if (throwWhenNotSuccessful) {
                    throw if (status == ClientErrorNotFound) ClientErrorNotFoundException(request, response)
                    else IppExchangeException(request, response)
                }
            }
        }
    }

    protected fun toHttpUri(ippUri: URI): URI = with(ippUri) {
        val scheme = scheme.replace("ipp", "http")
        val port = if (port == -1) 631 else port
        URI.create("$scheme://$host:$port$rawPath")
    }

    protected fun HttpURLConnection.configure(chunked: Boolean) {
        config.run {
            connectTimeout = timeout.toMillis().toInt()
            readTimeout = timeout.toMillis().toInt()
            userAgent?.let { setRequestProperty("User-Agent", it) }
            if (password != null) setRequestProperty("Authorization", authorization())
        }
        doOutput = true // POST
        if (chunked) setChunkedStreamingMode(0)
        setRequestProperty("Content-Type", APPLICATION_IPP)
        setRequestProperty("Accept", APPLICATION_IPP)
        setRequestProperty("Accept-Encoding", "identity") // avoid 'gzip' with Androids OkHttp
    }

    protected fun HttpURLConnection.validateResponse(
        request: IppRequest,
        contentStream: InputStream?,
        cause: Exception? = null
    ) =
        when {
            responseCode == 401 -> with(request) {
                "User '$requestingUserName' is unauthorized for operation '$operation'"
            }

            responseCode == 426 -> {
                "HTTP status $responseCode, $responseMessage, Try ipps://${request.printerUri.host}"
            }

            responseCode != 200 -> {
                "HTTP request failed: $responseCode, $responseMessage"
            }

            contentType != null && !contentType.startsWith(APPLICATION_IPP) -> {
                "Invalid Content-Type: $contentType"
            }

            cause != null -> cause.message

            else -> null
        }?.let {
            throw IppExchangeException(
                request,
                response = null,
                responseCode, // HTTP
                httpHeaderFields = headerFields,
                httpStream = contentStream,
                message = it,
                cause = cause
            )
        }

    protected fun decodeContentStream(
        request: IppRequest,
        httpStatus: Int,
        contentStream: InputStream
    ) = IppResponse().apply {
        try {
            read(contentStream)
        } catch (throwable: Throwable) {
            throw IppExchangeException(
                request, this, httpStatus, message = "failed to decode ipp response", cause = throwable
            ).apply {
                if (onExceptionSaveMessages)
                    saveMessages("decoding_ipp_response_${request.requestId}_failed")
            }
        }
    }

}