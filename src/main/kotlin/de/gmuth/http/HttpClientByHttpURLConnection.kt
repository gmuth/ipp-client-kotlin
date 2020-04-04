package de.gmuth.http

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.util.*
import javax.net.ssl.HttpsURLConnection

class HttpClientByHttpURLConnection(
        private val config: Http.Client.Config = Http.Client.Config(),
        var disableSSLCertificateValidation: Boolean = false

) : Http.Client {

    override fun post(uri: URI, content: Http.Content, auth: Http.Client.Auth?): Http.Response {
        if (uri.scheme in listOf("https", "ipps") && disableSSLCertificateValidation) {
            HttpsURLConnection.setDefaultSSLSocketFactory(SSLUtil.trustAllSSLContext.socketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier { hostname, session -> true }
            println("WARN: SSL certificate validation disabled")
        }
        with(uri.toURL().openConnection() as HttpURLConnection) {
            setConnectTimeout(config.timeout.toMillis().toInt())
            setDoOutput(true) // trigger POST method
            if (auth != null) {
                val basicAuth = with(auth) { Base64.getEncoder().encodeToString("$user:$password".toByteArray()) }
                setRequestProperty("Authorization", "Basic $basicAuth")
            }
            setRequestProperty("Content-Type", content.type)
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