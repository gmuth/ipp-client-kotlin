package de.gmuth.http

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpResponse

class HttpClientByJava11HttpClient(
        private val config: Http.Client.Config = Http.Client.Config()

) : Http.Client {

    override fun post(uri: URI, content: Http.Content, basicAuth: Http.Client.BasicAuth?): Http.Response {
        val httpRequest = java.net.http.HttpRequest.newBuilder()
                .timeout(config.timeout)
                .header("Content-Type", content.type)
                .POST(java.net.http.HttpRequest.BodyPublishers.ofInputStream { content.stream })
                .uri(uri).build()

        if(basicAuth != null) throw NotImplementedError("BasicAuth")

        val httpResponse = HttpClient.newBuilder().build()
                .send(httpRequest, HttpResponse.BodyHandlers.ofInputStream())

        with(httpResponse) {
            val responseContent = Http.Content(headers().firstValue("content-type").get(), body())
            return Http.Response(statusCode(), responseContent)
        }
    }

}