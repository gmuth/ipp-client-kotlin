package de.gmuth.http

/**
 * Copyright (c) 2020-2022 Gerhard Muth
 */

import de.gmuth.http.Http.Implementation.JavaHttpURLConnection
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.util.Base64.getEncoder
import javax.net.ssl.SSLContext

interface Http {

    data class Config(
        var timeout: Int = 30000, // milli seconds
        var userAgent: String? = null,
        var basicAuth: BasicAuth? = null,
        var sslContext: SSLContext? = null,
        // trust any certificate: sslContextForAnyCertificate()
        // use individual certificate: sslContext(loadCertificate(FileInputStream("printer.pem")))
        // use truststore: sslContext(loadKeyStore(FileInputStream("printer.jks"), "changeit"))
        var verifySSLHostname: Boolean = true,
        var accept: String? = null,
        var acceptEncoding: String? = null,
        var debugLogging: Boolean = false
    ) {
        fun trustAnyCertificate() {
            sslContext = SSLHelper.sslContextForAnyCertificate()
        }
    }

    data class BasicAuth(val user: String, val password: String) {
        fun encodeBase64(): String = "$user:$password".run {
            getEncoder().encodeToString(toByteArray())
        }
    }

    data class Response(
        val status: Int, val server: String?, val contentType: String?, val contentStream: InputStream?
    )

    abstract class Client(val config: Config) {
        abstract fun post(
            uri: URI, contentType: String, writeContent: (OutputStream) -> Unit, chunked: Boolean = false
        ): Response
    }

    // standard jvm implementations
    enum class Implementation(val createClient: (config: Config) -> Client) {
        JavaHttpURLConnection({ HttpURLConnectionClient(it) }),
        Java11HttpClient({ JavaHttpClient(it) })
    }

    companion object {
        var defaultImplementation: Implementation = JavaHttpURLConnection
    }

}