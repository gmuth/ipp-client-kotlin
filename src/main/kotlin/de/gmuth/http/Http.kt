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
            val server: String?, // header 'Server' is used to detect CUPS
            val contentType: String,
            val contentStream: InputStream
    )

    interface Client {
        val config: Config
        fun post(
                uri: URI,
                contentType: String,
                writeContent: (OutputStream) -> Unit,
                basicAuth: BasicAuth? = null
        ): Response
    }

}