package de.gmuth.http

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*

class HttpClientByJava11HttpClient(
        override val config: Http.Config = Http.Config()

) : Http.Client {

    private val java11HttpClient: HttpClient = with(HttpClient.newBuilder()) {
        if (config.trustAnySSLCertificate) {
            // -Djdk.internal.httpclient.disableHostnameVerification
            System.setProperty("jdk.internal.httpclient.disableHostnameVerification", true.toString())
            sslContext(AnyCertificateX509TrustManager.getNewSSLContextInstance())
        }
        build()
    }

    override fun post(uri: URI, requestContent: Http.Content, auth: Http.Auth?): Http.Response {
        val httpRequest = with(HttpRequest.newBuilder()) {
            timeout(config.timeout)
            header("Content-Type", requestContent.type)
            POST(HttpRequest.BodyPublishers.ofInputStream {
                requestContent.stream
            })
            uri(uri)
            if (auth != null) {
                val basicAuth = with(auth) {
                    Base64.getEncoder().encodeToString("$user:$password".toByteArray())
                }
                header("Authorization", "Basic $basicAuth")
            }
            build()
        }

        val httpResponse = java11HttpClient
                .send(httpRequest, HttpResponse.BodyHandlers.ofInputStream())

        return with(httpResponse) {
            Http.Response(
                    statusCode(),
                    Http.Content(
                            headers().firstValue("content-type").get(),
                            body()
                    )
            )
        }
    }
}