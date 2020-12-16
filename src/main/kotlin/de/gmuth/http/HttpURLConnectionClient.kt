package de.gmuth.http

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.log.Log
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.*
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection

class HttpURLConnectionClient(override val config: Http.Config = Http.Config()) : Http.Client {

    companion object {
        val log = Log.getWriter("HttpURLConnectionClient")
    }

    override fun post(uri: URI, contentType: String, writeContent: (OutputStream) -> Unit, basicAuth: Http.BasicAuth?): Http.Response {
        with(uri.toURL().openConnection() as HttpURLConnection) {
            if (this is HttpsURLConnection && config.sslSocketFactory != null) {
                sslSocketFactory = config.sslSocketFactory
                hostnameVerifier = HostnameVerifier { _, _ -> true }
            }
            connectTimeout = config.timeout
            readTimeout = config.timeout
            doOutput = true // trigger POST method
            basicAuth?.let {
                val basicAuthEncoded = with(basicAuth) {
                    Base64.getEncoder().encodeToString("$user:$password".toByteArray())
                }
                setRequestProperty("Authorization", "Basic $basicAuthEncoded")
            }
            setRequestProperty("User-Agent", config.userAgent)
            setRequestProperty("Content-Type", contentType)
            // chunked streaming mode can cause: "HttpRetryException: cannot retry due to server authentication, in streaming mode"
            //setChunkedStreamingMode(0) // enable chunked transfer
            writeContent(outputStream)
            for ((key, values) in headerFields) {
                log.debug { "$key = $values" }
            }
            val contentResponseStream = try {
                inputStream
            } catch (exception: Exception) {
                log.error { "$exception" }
                errorStream
            }
            return Http.Response(
                    responseCode,
                    getHeaderField("Server"),
                    getHeaderField("Content-Type"),
                    contentResponseStream
            )
        }
    }
}