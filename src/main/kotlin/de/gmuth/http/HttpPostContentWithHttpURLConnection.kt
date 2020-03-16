package de.gmuth.http

/**
 * Author: Gerhard Muth
 */

import java.net.HttpURLConnection
import java.net.URI
import java.time.Duration

class HttpPostContentWithHttpURLConnection : HttpPostContent {

    override fun post(uri: URI, content: HttpContent): HttpResponse {
        with(uri.toURL().openConnection() as HttpURLConnection) {
            setConnectTimeout(Duration.ofSeconds(5).toMillis().toInt())
            setDoOutput(true)
            setRequestProperty("Content-Type", content.type)
            content.stream.copyTo(outputStream)
            outputStream.close()
            val responseContent = HttpContent(getHeaderField("Content-Type"), inputStream)
            return HttpResponse(responseCode, responseContent)
        }
    }

}