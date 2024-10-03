package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2024 Gerhard Muth
 */

import de.gmuth.ipp.client.IppOperationException.ClientErrorNotFoundException
import de.gmuth.ipp.core.IppException
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
import java.util.logging.Level.FINEST
import java.util.logging.Level.SEVERE
import java.util.logging.Logger
import java.util.logging.Logger.getLogger
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import kotlin.io.path.createTempDirectory

typealias IppResponseInterceptor = (request: IppRequest, response: IppResponse) -> Unit

open class IppClient(val config: IppConfig = IppConfig()) {
    protected val logger: Logger = getLogger(javaClass.name)

    var responseInterceptor: IppResponseInterceptor? = null
    var saveMessages: Boolean = false
    var saveMessagesDirectory = createTempDirectory().toFile()
    var onExceptionSaveMessages: Boolean = false
    var throwWhenNotSuccessful: Boolean = true
    var disconnectAfterHttpPost: Boolean = false

    fun basicAuth(user: String, password: String) {
        config.userName = user
        config.password = password
    }

    companion object {
        const val APPLICATION_IPP = "application/ipp"
    }

    //-----------------
    // Build IppRequest
    //-----------------

    private val requestCounter = AtomicInteger(1)

    fun ippRequest(
        operation: IppOperation,
        printerUri: URI? = null,
        requestedAttributes: Collection<String>? = null,
        userName: String? = config.userName,
        naturalLanguage: String = config.naturalLanguage,
    ) = IppRequest(
        operation,
        printerUri,
        requestedAttributes,
        userName,
        config.ippVersion,
        requestCounter.getAndIncrement(),
        config.charset,
        naturalLanguage
    )

    //------------------------------------
    // Exchange IppRequest for IppResponse
    //------------------------------------

    @JvmOverloads
    fun exchange(request: IppRequest, ippUri: URI = request.printerOrJobUri) = with(request) {
        logger.fine { "Send '$operation' request to $ippUri" }
        val httpUri = with(ippUri) {
            URI(
                scheme.replace("ipp", "http"), userInfo, host,
                if (port == -1) 631 else port, path, query, fragment
            )
        }
        httpPost(httpUri, request).also {
            logger.finer { "Exchanged request #$requestId @${httpUri.host}: $request => $it" }
            log(logger, FINEST, ">>> ") //  this=request
            it.log(logger, FINEST, "<<< ") // it=response
            fun file(extension: String) =
                File(saveMessagesDirectory, "%03d-%s.%s".format(requestId, operation, extension))
            if (saveMessages) {
                saveBytes(file("req"))
                saveText(file("req.txt"))
                it.saveBytes(file("res"))
                it.saveText(file("res.txt"))
            }
            responseInterceptor?.invoke(request, it)
            validateIppResponse(request, it)
        }
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
            try {
                request.write(outputStream)
                val responseContentStream = try {
                    validateHttpResponse(request, inputStream)
                    inputStream
                } catch (ioException: IOException) {
                    validateHttpResponse(request, errorStream, ioException)
                    errorStream
                }
                return decodeContentStream(request, responseContentStream)
                    .apply { httpServer = getHeaderField("Server") }
            } catch (ioException: IOException) {
                val compression =
                    if (request.operationGroup.containsKey("compression")) ", compression=${request.compression.name}"
                    else ""
                throw IppException("HTTP communication with $httpUri failed$compression", ioException)
            } finally {
                if (disconnectAfterHttpPost) disconnect()
            }
        }
    }

    private fun validateIppResponse(request: IppRequest, response: IppResponse) = response.run {
        if (status == ClientErrorBadRequest) {
            request.log(logger, SEVERE, prefix = "REQUEST: ")
            response.log(logger, SEVERE, prefix = "RESPONSE: ")
        }
        if (containsGroup(Unsupported)) unsupportedGroup.values.forEach { logger.warning() { "Unsupported: $it" } }
        if (!isSuccessful()) {
            IppRegistrationsSection2.validate(request)
            val exception =
                if (status == ClientErrorNotFound) ClientErrorNotFoundException(request, response)
                else IppOperationException(request, response)
            if (onExceptionSaveMessages)
                exception.saveMessages("${request.operation}_${status}_${request.requestId}")
            if (throwWhenNotSuccessful)
                throw exception
        }
    }

    private fun HttpURLConnection.configure(chunked: Boolean) {
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

    private fun HttpURLConnection.validateHttpResponse(
        request: IppRequest,
        contentStream: InputStream?,
        exception: Exception? = null
    ) = when {
        responseCode in 300..308 -> {
            "HTTP redirect: $responseCode, $responseMessage, Location: ${getHeaderField("Location")}"
        }

        responseCode == 401 && request.operationGroup.containsKey("requesting-user-name") -> with(request) {
            "User \"$requestingUserName\" is not authorized for operation $operation on $printerOrJobUri"
        }

        responseCode == 401 -> with(request) { "Not authorized for operation $operation on $printerOrJobUri (userName required)" }
        responseCode == 426 -> "HTTP status $responseCode, $responseMessage, Try ipps://${request.printerOrJobUri.host}"
        responseCode != 200 -> "HTTP request failed: $responseCode, $responseMessage"
        contentType != null && !contentType.startsWith(APPLICATION_IPP) -> "Invalid Content-Type: $contentType"
        exception != null -> exception.message
        else -> null // no issues found
    }?.let {
        throw HttpPostException(
            request,
            httpStatus = responseCode,
            httpHeaderFields = headerFields,
            httpStream = contentStream,
            message = it,
            cause = exception
        )
    }

    private fun decodeContentStream(request: IppRequest, contentStream: InputStream) = IppResponse().apply {
        try {
            read(contentStream)
        } catch (throwable: Throwable) {
            throw IppOperationException(request, this, "Failed to decode ipp response", throwable).apply {
                if (onExceptionSaveMessages)
                    saveMessages("decoding_ipp_response_${request.requestId}_failed")
            }
        }
    }
}