package de.gmuth.http

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.*
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection

class HttpURLConnectionClient(val config: Http.Config = Http.Config()) : Http.Client {
    override fun post(uri: URI, contentType: String, writeContent: (OutputStream) -> Unit, basicAuth: Http.BasicAuth?): Http.Response {
        with(uri.toURL().openConnection() as HttpURLConnection) {
            if (this is HttpsURLConnection && config.trustAnySSLCertificate) {
                sslSocketFactory = AnyCertificateX509TrustManager.socketFactory
                hostnameVerifier = HostnameVerifier { _, _ -> true }
            }
            connectTimeout = config.timeout
            doOutput = true // trigger POST method
            if (basicAuth != null) {
                if (uri.scheme in listOf("http", "ipp")) {
                    println("WARN: '${uri.scheme}' does not protect credentials")
                }
                val basicAuth = with(basicAuth) {
                    Base64.getEncoder().encodeToString("$user:$password".toByteArray())
                }
                setRequestProperty("Authorization", "Basic $basicAuth")
            }
            setRequestProperty("Content-Type", contentType)
            // chunked streaming mode can cause: "HttpRetryException: cannot retry due to server authentication, in streaming mode"
            //setChunkedStreamingMode(0) // enable chunked transfer
            writeContent(outputStream)
            val contentResponseStream = try {
                inputStream
            } catch (ioException: IOException) {
                println("responseCode = $responseCode")
                for ((key, values) in headerFields) {
                    println("$key = $values")
                }
                println("ERROR: $ioException")
                errorStream
            }
            return Http.Response(responseCode, getHeaderField("Content-Type"), contentResponseStream)
        }
    }
}