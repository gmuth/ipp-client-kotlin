package de.gmuth.http

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.log.JulAdapter
import de.gmuth.log.JulHandler
import de.gmuth.log.Logging
import de.gmuth.log.Logging.LogLevel.*
import de.gmuth.log.Logging.createLogger
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URI
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection

class HttpURLConnectionClient(config: Http.Config = Http.Config()) : Http.Client(config) {

    companion object {
        val log = Logging.getLogger {}
    }

    init {
        log.debug { "HttpURLConnectionClient created" }
        if (config.debugLogging && createLogger != ::JulAdapter) {
            // The JulHandler forwards ALL jul message to Logging
            JulHandler.addToJulLogger("sun.net.www.protocol.http")
            // The JulHandler does NOT use the jul output config
            Logging.getLogger("sun.net.www.protocol.http.HttpURLConnection").logLevel = TRACE
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
                    responseCode < 300 -> DEBUG
                    responseCode in 400..499 -> INFO
                    else -> WARN
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