package de.gmuth.http

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.InputStream
import java.io.OutputStream
import java.net.URI

interface Http {

    data class Config(
            var timeout: Int = 3000, // milli seconds
            var trustAnySSLCertificate: Boolean = true
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
        fun post(
                uri: URI,
                contentType: String,
                writeContent: (OutputStream) -> Unit,
                basicAuth: BasicAuth? = null
        ): Response
    }

}