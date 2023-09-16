package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.client.IppExchangeException.ClientErrorNotFoundException
import de.gmuth.ipp.core.IppException
import de.gmuth.ipp.core.IppOperation
import de.gmuth.ipp.core.IppRequest
import de.gmuth.ipp.core.IppResponse
import de.gmuth.ipp.core.IppStatus.ClientErrorBadRequest
import de.gmuth.ipp.core.IppStatus.ClientErrorNotFound
import de.gmuth.ipp.core.IppTag.Unsupported
import de.gmuth.ipp.iana.IppRegistrationsSection2
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level.SEVERE
import java.util.logging.Logger.getLogger
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection

typealias IppResponseInterceptor = (request: IppRequest, response: IppResponse) -> Unit

open class IppClient(val config: IppConfig = IppConfig()) {
    val log = getLogger(javaClass.name)
    var saveMessages: Boolean = false
    var saveMessagesDirectory = File("ipp-messages")
    var responseInterceptor: IppResponseInterceptor? = null
    //var server: String? = null

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

    private val requestCounter = AtomicInteger(1)

    fun ippRequest(
        operation: IppOperation,
        printerUri: URI,
        jobId: Int? = null,
        requestedAttributes: List<String>? = null,
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
    // exchange IppRequest for IppResponse
    //------------------------------------

    open fun exchange(request: IppRequest, throwWhenNotSuccessful: Boolean = true): IppResponse {
        val ippUri: URI = request.printerUri
        log.finer { "send '${request.operation}' request to $ippUri" }

        val response = postRequest(toHttpUri(ippUri), request)
        log.fine { "$ippUri: $request => $response" }

        if (saveMessages) {
            val messageSubDirectory = File(saveMessagesDirectory, ippUri.host).apply {
                if (!mkdirs() && !isDirectory) throw IppException("failed to create directory: $path")
            }

            fun file(suffix: String) = File(messageSubDirectory, "${request.requestId}-${request.operation}.$suffix")
            request.saveRawBytes(file("request"))
            response.saveRawBytes(file("response"))
        }

        responseInterceptor?.invoke(request, response)

        with(response) {
            if (status == ClientErrorBadRequest) request.log(log, SEVERE, prefix = "BAD-REQUEST: ")
            if (containsGroup(Unsupported)) unsupportedGroup.values.forEach { log.warning() { "unsupported: $it" } }
            if (!isSuccessful()) {
                IppRegistrationsSection2.validate(request)
                if (throwWhenNotSuccessful)
                    throw if (status == ClientErrorNotFound) ClientErrorNotFoundException(request, response)
                    else IppExchangeException(request, response)
            }
        }
        return response
    }

    fun toHttpUri(ippUri: URI): URI = with(ippUri) {
        val scheme = scheme.replace("ipp", "http")
        val port = if (port == -1) 631 else port
        URI.create("$scheme://$host:$port$rawPath")
    }

    open fun postRequest(httpUri: URI, request: IppRequest): IppResponse {
        with(httpUri.toURL().openConnection() as HttpURLConnection) {
            if (this is HttpsURLConnection && config.sslContext != null) {
                sslSocketFactory = config.sslContext!!.socketFactory
                if (!config.verifySSLHostname) hostnameVerifier = HostnameVerifier { _, _ -> true }
            }
            config.run {
                connectTimeout = timeout.toMillis().toInt()
                readTimeout = timeout.toMillis().toInt()
                userAgent?.let { setRequestProperty("User-Agent", it) }
                if (password != null) setRequestProperty("Authorization", authorization())
            }
            doOutput = true // POST
            setRequestProperty("Content-Type", APPLICATION_IPP)
            setRequestProperty("Accept", APPLICATION_IPP)
            setRequestProperty("Accept-Encoding", "identity") // avoid 'gzip' with Androids OkHttp
            if (request.hasDocument()) setChunkedStreamingMode(0) // send document in chunks
            request.write(outputStream)
            val responseContentStream = try {
                inputStream
            } catch (throwable: Throwable) {
                errorStream
            }

            // error handling
            when {
                responseCode == 401 -> with(request) {
                    "User '$requestingUserName' is unauthorized for operation '$operation'"
                }
                responseCode != 200 -> {
                    "HTTP request to $httpUri failed: $responseCode, $responseMessage"
                }
                contentType != null && !contentType.startsWith(APPLICATION_IPP) -> {
                    "Invalid Content-Type: $contentType"
                }
                else -> null
            }?.let {
                throw IppExchangeException(
                    request,
                    response = null,
                    responseCode,
                    httpHeaderFields = headerFields,
                    httpStream = responseContentStream,
                    message = it
                )
            }

            // decode ipp message
            return IppResponse().apply {
                try {
                    read(responseContentStream)
                } catch (throwable: Throwable) {
                    throw IppExchangeException(
                        request, this, responseCode, message = "failed to decode ipp response", cause = throwable
                    ).apply {
                        saveMessages("decoding_ipp_response_${request.requestId}_failed")
                    }
                }
            }
        }
    }
}