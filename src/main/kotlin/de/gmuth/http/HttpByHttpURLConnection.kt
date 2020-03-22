package de.gmuth.http

/**
 * Author: Gerhard Muth
 */

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.time.Duration

class HttpByHttpURLConnection : Http {

    override fun post(uri: URI, content: Http.Content): Http.Response {
        with(uri.toURL().openConnection() as HttpURLConnection) {
            setConnectTimeout(Duration.ofSeconds(5).toMillis().toInt())
            setDoOutput(true)
            setRequestProperty("Content-Type", content.type)
            content.stream.copyTo(outputStream)
            outputStream.close()
            // read response
            val contentStream = try {
                inputStream
            } catch(ioException: IOException) {
                errorStream
            }
            val responseContent =  Http.Content(getHeaderField("Content-Type"), contentStream)
            return Http.Response(responseCode, responseContent)
        }
    }

}