package de.gmuth.http

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

class HttpClientByHttpURLConnection(
        private val config: Http.Client.Config = Http.Client.Config()
) : Http.Client {

    override fun post(uri: URI, content: Http.Content): Http.Response {
        with(uri.toURL().openConnection() as HttpURLConnection) {
            setConnectTimeout(config.timeout.toMillis().toInt())
            setDoOutput(true) // trigger POST method
            setChunkedStreamingMode(0) // enable chunked transfer
            setRequestProperty("Content-Type", content.type)
            content.stream.copyTo(outputStream)
            // read response
            val contentStream = try {
                inputStream
            } catch (ioException: IOException) {
                errorStream
            }
            val responseContent = Http.Content(getHeaderField("Content-Type"), contentStream)
            return Http.Response(responseCode, responseContent)
        }
    }

}