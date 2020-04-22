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
        override val config: Http.Client.Config = Http.Client.Config()

) : Http.Client {

    override fun post(uri: URI, content: Http.Content, auth: Http.Auth?): Http.Response {
        if (uri.scheme in listOf("https", "ipps") && config.trustAnySSLCertificate) {
            HttpsURLConnection.setDefaultSSLSocketFactory(SSLUtil.trustAllSSLContext.socketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
            println("WARN: SSL certificate validation disabled")
        }
        with(uri.toURL().openConnection() as HttpURLConnection) {
            connectTimeout = config.timeout.toMillis().toInt()
            doOutput = true // trigger POST method
            if (auth != null) {
                if (uri.scheme in listOf("http", "ipp")) {
                    println("WARN: '${uri.scheme}' is not secure for authentication")
                }
                val basicAuth = with(auth) { Base64.getEncoder().encodeToString("$user:$password".toByteArray()) }
                setRequestProperty("Authorization", "Basic $basicAuth")
            }
            setRequestProperty("Content-Type", content.type)
            // setChunkedStreamingMode(0) // enable chunked transfer -- HttpRetryException: cannot retry due to server authentication, in streaming mode
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