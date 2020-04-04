package de.gmuth.http

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.InputStream
import java.net.URI
import java.time.Duration

interface Http {

    class Content(val type: String, val stream: InputStream)
    class Response(val status: Int, val content: Content)
    class Auth(val user: String, val password: String)

    interface Client {
        data class Config(val timeout: Duration = Duration.ofSeconds(5))

        fun post(uri: URI, content: Content, auth: Auth? = null): Response
    }

}