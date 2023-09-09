package de.gmuth.http

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.log.debug
import de.gmuth.log.error
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.logging.Level
import java.util.logging.Logger.getLogger
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection

class HttpURLConnectionClient(config: Http.Config = Http.Config()) : Http.Client(config) {

    val log = getLogger(javaClass.name)

    init {
        log.debug { "HttpURLConnectionClient created" }
        /** if (config.debugLogging && createLogger != ::JulAdapter) {
        // The JulHandler forwards ALL jul message to Logging
        JulRedirectHandler.addToJulLogger("sun.net.www.protocol.http")
        // The JulHandler does NOT use the jul output config
        Logging.getLogger("sun.net.www.protocol.http.HttpURLConnection").logLevel = TRACE
        } **/
        if (config.debugLogging) {
            getLogger("sun.net.www.protocol.http.HttpURLConnection").level = Level.FINER
        }
    }

    override fun post(
        uri: URI, contentType: String, writeContent: (OutputStream) -> Unit, chunked: Boolean
    ): Http.Response {
        with(uri.toURL().openConnection() as HttpURLConnection) {
            if (this is HttpsURLConnection && config.sslContext != null) {
                sslSocketFactory = config.sslContext!!.socketFactory
                if (!config.verifySSLHostname) hostnameVerifier = HostnameVerifier { _, _ -> true }
            }
            doOutput = true // trigger POST method
            config.run {
                connectTimeout = timeout
                readTimeout = timeout
                accept?.let { setRequestProperty("Accept", it) }
                acceptEncoding?.let { setRequestProperty("Accept-Encoding", it) }
                basicAuth?.let { setRequestProperty("Authorization", it.authorization()) }
                userAgent?.let { setRequestProperty("User-Agent", it) }
            }
            setRequestProperty("Content-Type", contentType)
            if (chunked) setChunkedStreamingMode(0)
            writeContent(outputStream)
            for ((key, values) in headerFields) {
                val logLevel = when {
                    responseCode < 300 -> Level.FINE
                    responseCode in 400..499 -> Level.INFO
                    else -> Level.WARNING
                }
                log.log(logLevel) { "$key = $values" }
            }
            val responseStream = try {
                inputStream
            } catch (exception: Exception) {
                log.error { "http exception: $responseCode $responseMessage" }
                errorStream
            }
            return Http.Response(
                responseCode, getHeaderField("Server"), getHeaderField("Content-Type"), responseStream
            )
        }
    }
}