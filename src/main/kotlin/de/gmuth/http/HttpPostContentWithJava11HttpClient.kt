package de.gmuth.http

/**
 * Author: Gerhard Muth
 */

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.time.Duration

class HttpPostContentWithJava11HttpClient : HttpPostContent {

    override fun post(uri: URI, content: HttpContent): HttpResponse {
        val httpRequest = HttpRequest.newBuilder()
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", content.type)
                .POST(HttpRequest.BodyPublishers.ofInputStream { content.stream })
                .uri(uri).build()

        val httpResponse = HttpClient.newBuilder()
                .build()
                .send(httpRequest, java.net.http.HttpResponse.BodyHandlers.ofInputStream())

        if (httpResponse.statusCode() != 200) {
            for ((key, values) in httpResponse.headers().map())
                println("$key: ${values.joinToString()}")
        }

        with(httpResponse) {
            val responseContent = HttpContent(headers().firstValue("content-type").get(), body())
            return HttpResponse(statusCode(), responseContent)
        }
    }
}