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
        override val config: Http.Client.Config = Http.Client.Config()

) : Http.Client {

    override fun post(uri: URI, content: Http.Content, auth: Http.Auth?): Http.Response {
        val httpClientBuilder = HttpClient.newBuilder()
        if (uri.scheme in listOf("https", "ipps") && config.trustAnySSLCertificate) {
            // -Djdk.internal.httpclient.disableHostnameVerification
            System.getProperties().setProperty("jdk.internal.httpclient.disableHostnameVerification", true.toString())
            httpClientBuilder.sslContext(SSLUtil.trustAllSSLContext)
            println("WARN: SSL certificate validation disabled")
        }

        val httpRequestBuilder = HttpRequest.newBuilder()
                .timeout(config.timeout)
                .header("Content-Type", content.type)
                .POST(java.net.http.HttpRequest.BodyPublishers.ofInputStream { content.stream })
                .uri(uri)

        if (auth != null) {
            val basicAuth = with(auth) { Base64.getEncoder().encodeToString("$user:$password".toByteArray()) }
            httpRequestBuilder.header("Authorization", "Basic $basicAuth")
        }

        val httpResponse = httpClientBuilder.build()
                .send(httpRequestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream())

        with(httpResponse) {
            val responseContent = Http.Content(headers().firstValue("content-type").get(), body())
            return Http.Response(statusCode(), responseContent)
        }
    }

}