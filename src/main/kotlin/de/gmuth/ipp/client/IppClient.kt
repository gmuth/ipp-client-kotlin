package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2025 Gerhard Muth
 */

import de.gmuth.ipp.client.IppOperationException.ClientErrorNotFoundException
import de.gmuth.ipp.core.IppOperation
import de.gmuth.ipp.core.IppRequest
import de.gmuth.ipp.core.IppResponse
import de.gmuth.ipp.core.IppStatus.ClientErrorBadRequest
import de.gmuth.ipp.core.IppStatus.ClientErrorNotFound
import de.gmuth.ipp.core.IppTag.Unsupported
import de.gmuth.ipp.core.appendAttributeIfGroupContainsKey
import de.gmuth.ipp.iana.IppRegistrationsSection2
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level
import java.util.logging.Level.FINEST
import java.util.logging.Level.WARNING
import java.util.logging.Logger
import java.util.logging.Logger.getLogger
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import kotlin.io.path.createTempDirectory

typealias IppResponseInterceptor = (request: IppRequest, response: IppResponse) -> Unit

open class IppClient(val config: IppConfig = IppConfig()) {
    protected val logger: Logger = getLogger(javaClass.name)

    var responseInterceptor: IppResponseInterceptor? = null
    var saveEvents: Boolean = false
    var saveMessages: Boolean = false
    var saveDocuments: Boolean = false
    var saveMessagesDirectory: Path = createTempDirectory()
    var onExceptionSaveMessages: Boolean = false
    var throwWhenNotSuccessful: Boolean = true
    var disconnectAfterHttpPost: Boolean = false
    var defaultPrinterUri: URI? = URI.create("ipp://ippbin.net:12345")
    var onExchangeOverrideRequestPrinterOrJobUri: URI? = null // Useful for reverse proxies or NAT
    var onExchangeLogRequestAndResponseWithLevel: Level = FINEST

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

    @JvmOverloads
    fun ippRequest(
        operation: IppOperation,
        printerUri: URI? = defaultPrinterUri,
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
        naturalLanguage,
        config.userAgent
    )

    fun wrap(request: IppRequest, response: IppResponse): IppRequest = ippRequest(request.operation)
        .apply { documentInputStream = ByteArrayInputStream(response.rawBytes) }

    //------------------------------------
    // Exchange IppRequest for IppResponse
    //------------------------------------

    @JvmOverloads
    fun exchange(
        request: IppRequest,
        ippUri: URI = onExchangeOverrideRequestPrinterOrJobUri ?: request.printerOrJobUri
    ) =
        with(request) {
            val httpUri = with(ippUri) {
                URI(
                    scheme.replace("ipp", "http"), userInfo, host,
                    if (port == -1) 631 else port, path, query, fragment
                )
            }
            httpPost(httpUri, request).also {
                logger.log(onExchangeLogRequestAndResponseWithLevel, "-".repeat(50) + " request to $ippUri")
                log(logger, onExchangeLogRequestAndResponseWithLevel) //  this=request
                logger.log(onExchangeLogRequestAndResponseWithLevel, "-".repeat(50) + " response from $ippUri")
                it.log(logger, onExchangeLogRequestAndResponseWithLevel) // it=response
                IppRequestExchangedEvent(request, it, ippUri).run {
                    logger.fine { toString() }
                    if (saveEvents || saveDocuments || saveMessages)
                        save(saveMessagesDirectory, saveEvents, saveDocuments, saveMessages)
                }
                responseInterceptor?.invoke(request, it)
                validateIppResponse(request, it)
            }
        }

    fun exchangeForEvent(
        request: IppRequest,
        ippUri: URI = onExchangeOverrideRequestPrinterOrJobUri ?: request.printerOrJobUri
    ) =
        IppRequestExchangedEvent(request, exchange(request, ippUri), ippUri)

    @SuppressWarnings("kotlin:S108")
    fun exchangeWrapped(request: IppRequest) = exchange(request).also {
        try {
            exchange(wrap(request, it))
        } catch (throwable: Throwable) {
            logger.finer(throwable.cause?.toString())
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
            configure(
                chunked = request.hasDocument(),
                userAgent = request.httpUserAgent
            )
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
                throw HttpPostException(
                    request, cause = ioException, message = StringBuilder().apply {
                        with(ioException) { append("$httpUri: ${javaClass.simpleName}: $message") }
                        appendAttributeIfGroupContainsKey(request.operationGroup, "compression")
                    }.toString()
                )
            } finally {
                if (disconnectAfterHttpPost) disconnect()
            }
        }
    }

    private fun validateIppResponse(request: IppRequest, response: IppResponse) = response.run {
        if (status == ClientErrorBadRequest) {
            request.log(logger, WARNING, prefix = "REQUEST: ")
            response.log(logger, WARNING, prefix = "RESPONSE: ")
        }
        if (containsGroup(Unsupported)) unsupportedGroup.values.forEach { logger.warning() { "Unsupported: $it" } }
        if (!isSuccessful()) {
            IppRegistrationsSection2.validate(request)
            val exception =
                if (status == ClientErrorNotFound) ClientErrorNotFoundException(request, response)
                else IppOperationException(request, response)
            if (onExceptionSaveMessages)
                exception.saveMessages(fileNameWithoutSuffix = "${request.operation}_${status}_${request.requestId}")
            if (throwWhenNotSuccessful)
                throw exception
        }
    }

    private fun HttpURLConnection.configure(chunked: Boolean, userAgent: String?) {
        config.run {
            connectTimeout = timeout.toMillis().toInt()
            readTimeout = timeout.toMillis().toInt()
            if (password != null) setRequestProperty("Authorization", authorization())
        }
        doOutput = true // POST
        if (chunked) setChunkedStreamingMode(0)
        setRequestProperty("Content-Type", APPLICATION_IPP)
        setRequestProperty("Accept", APPLICATION_IPP)
        setRequestProperty("Accept-Encoding", "identity") // avoid 'gzip' with Androids OkHttp
        userAgent?.let { setRequestProperty("User-Agent", it) }
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
        responseCode == 426 -> {
            val upgrade = getHeaderField("Upgrade")
            "HTTP status $responseCode, $responseMessage, Try ipps://${request.printerOrJobUri.host}, Upgrade: $upgrade"
        }

        responseCode != 200 -> "HTTP request failed: $responseCode, $responseMessage"
        contentType != null && !contentType.startsWith(APPLICATION_IPP) -> "Invalid Content-Type: $contentType"
        exception != null -> exception.message
        else -> {
            headerFields.forEach { (key, values) -> logger.finest { "$key: $values" } }
            null // no issues found
        }
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
                if (onExceptionSaveMessages) saveMessages(
                    fileNameWithoutSuffix = "decoding_ipp_response_${request.requestId}_from_${request.operation}_failed"
                )
            }
        }
    }
}