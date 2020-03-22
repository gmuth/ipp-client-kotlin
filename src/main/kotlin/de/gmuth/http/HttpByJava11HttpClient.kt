package de.gmuth.http

/**
 * Author: Gerhard Muth
 */

import java.net.URI
import java.time.Duration

class HttpByJava11HttpClient : Http {

    override fun post(uri: URI, content: Http.Content): Http.Response {
        val httpRequest = java.net.http.HttpRequest.newBuilder()
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", content.type)
                .POST(java.net.http.HttpRequest.BodyPublishers.ofInputStream { content.stream })
                .uri(uri).build()

        val httpResponse = java.net.http.HttpClient.newBuilder().build()
                .send(httpRequest, java.net.http.HttpResponse.BodyHandlers.ofInputStream())

        with(httpResponse) {
            val responseContent = Http.Content(headers().firstValue("content-type").get(), body())
            return Http.Response(statusCode(), responseContent)
        }
    }

}