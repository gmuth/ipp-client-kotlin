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
            var timeout: Int = 3000, // milli seconds
            var sslSocketFactory: SSLSocketFactory? = null
            // sslSocketFactoryForAnyCertificate()
            // sslSocketFactory(loadCertificate(FileInputStream("printer.pem")))
            // sslSocketFactory(loadTrustStore(FileInputStream("printer.jks"), "changeit"))
    )

    data class BasicAuth(
            val user: String,
            val password: String
    )

    data class Response(
            val status: Int,
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