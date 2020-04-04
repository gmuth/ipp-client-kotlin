package de.gmuth.http

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


class HttpClientByHttpURLConnection(
        private val config: Http.Client.Config = Http.Client.Config(),
        var disableSSLCertificateValidation: Boolean = false

) : Http.Client {

    override fun post(uri: URI, content: Http.Content, basicAuth: Http.Client.BasicAuth?): Http.Response {
        if (uri.scheme in listOf("https", "ipps") && disableSSLCertificateValidation)
            disableSSLCertificateValidation()

        with(uri.toURL().openConnection() as HttpURLConnection) {
            setConnectTimeout(config.timeout.toMillis().toInt())
            setDoOutput(true) // trigger POST method
            if (basicAuth != null) {
                val basicAuthBase64 = with(basicAuth) { Base64.getEncoder().encodeToString("$user:$password".toByteArray()) }
                setRequestProperty("Authorization", "Basic $basicAuthBase64")
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

    private fun disableSSLCertificateValidation() {
        val trustAllCerts: Array<TrustManager> = arrayOf(object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()

            @Throws(CertificateException::class)
            override fun checkClientTrusted(certificates: Array<X509Certificate?>?, string: String?) {
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(certificates: Array<X509Certificate?>?, string: String?) {
            }
        })
        val sslContext = SSLContext.getInstance("TLS").apply { init(null, trustAllCerts, SecureRandom()) }
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory())
        HttpsURLConnection.setDefaultHostnameVerifier { hostname, session -> true }
        println("WARN: certificate validation disabled for SSL")
    }

}