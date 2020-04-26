package de.gmuth.http

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.util.*
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection

class HttpClientByHttpURLConnection(
        override val config: Http.Config = Http.Config()

) : Http.Client {
    override fun post(uri: URI, content: Http.Content, auth: Http.Auth?): Http.Response {
        with(uri.toURL().openConnection() as HttpURLConnection) {
            if (this is HttpsURLConnection && config.trustAnySSLCertificate) {
                sslSocketFactory = AnyCertificateX509TrustManager.socketFactory
                hostnameVerifier = HostnameVerifier { _, _ -> true }
            }
            connectTimeout = config.timeout.toMillis().toInt()
            doOutput = true // trigger POST method
            if (auth != null) {
                if (uri.scheme in listOf("http", "ipp") && auth != null) {
                    println("WARN: '${uri.scheme}' does not protect credentials")
                }
                val basicAuth = with(auth) {
                    Base64.getEncoder().encodeToString("$user:$password".toByteArray())
                }
                setRequestProperty("Authorization", "Basic $basicAuth")
            }
            setRequestProperty("Content-Type", content.type)
            // chunked streaming mode can cause: "HttpRetryException: cannot retry due to server authentication, in streaming mode"
            //setChunkedStreamingMode(0) // enable chunked transfer
            content.stream.copyTo(outputStream)
            val contentStream = try {
                inputStream
            } catch (ioException: IOException) {
                println("responseCode = $responseCode")
                for ((key, values) in headerFields) {
                    println("$key = $values")
                }
                println("ERROR: $ioException")
                errorStream
            }
            val responseContent = Http.Content(getHeaderField("Content-Type"), contentStream)
            return Http.Response(responseCode, responseContent)
        }
    }
}