package de.gmuth.http

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.InputStream
import java.net.URI

interface Http {

    data class Content(val type: String, val stream: InputStream)

    data class Response(val status: Int, val content: Content)

    data class Auth(val user: String, val password: String)

    data class Config(
            var timeout: Int = 3000, // milli seconds
            var trustAnySSLCertificate: Boolean = true
    )

    interface Client {
        fun post(uri: URI, content: Content, auth: Auth? = null): Response
    }

}
