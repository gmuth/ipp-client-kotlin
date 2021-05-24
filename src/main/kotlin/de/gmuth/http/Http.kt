package de.gmuth.http

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import javax.net.ssl.SSLSocketFactory

interface Http {

    data class Config(
            var timeout: Int = 30000, // milli seconds
            var userAgent: String = "ipp-client-kotlin/2.0",
            var sslSocketFactory: SSLSocketFactory? = null
            // trust any certificate: sslSocketFactoryForAnyCertificate()
            // use individual certificate: sslSocketFactory(loadCertificate(FileInputStream("printer.pem")))
            // use truststore: sslSocketFactory(loadTrustStore(FileInputStream("printer.jks"), "changeit"))
    )

    data class BasicAuth(
            val user: String,
            val password: String
    )

    data class Response(
            val status: Int,
            val server: String?,
            val contentType: String?,
            val contentStream: InputStream?
    ) {
        fun isOK() = status == 200
        fun hasContent() = contentStream != null
        fun readTextContent() = contentStream!!.bufferedReader().use { it.readText() }
        fun contentTypeIsText() = contentType != null && contentType.startsWith("text")
        fun textContent() = if (hasContent() && contentTypeIsText()) "\n" + readTextContent() else ""
    }

    abstract class Client(val config: Config = Config()) {
        abstract fun post(
                uri: URI,
                contentType: String,
                writeContent: (OutputStream) -> Unit,
                basicAuth: BasicAuth? = null
        ): Response
    }

}