package de.gmuth.http

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.InputStream
import java.net.URI
import java.time.Duration

interface Http {

    data class Content(val type: String, val stream: InputStream)

    data class Response(val status: Int, val content: Content)

    data class Auth(val user: String, val password: String)

    data class Config(
            var timeout: Duration = Duration.ofSeconds(3),
            var trustAnySSLCertificate: Boolean = true
    )

    interface Client {
        val config: Config
        fun post(uri: URI, content: Content, auth: Auth? = null): Response
    }

}