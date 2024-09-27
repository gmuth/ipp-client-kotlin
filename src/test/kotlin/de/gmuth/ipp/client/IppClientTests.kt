package de.gmuth.ipp.client

/**
 * Copyright (c) 2023-2024 Gerhard Muth
 */

import org.junit.Test
import java.net.URI
import kotlin.test.assertEquals

class IppClientTests {
    val ippClient = IppClient()

    @Test
    fun toHttpUriWithEncodedSpace_v1() {
        val ippUri = URI.create("ipp://0/PDF%20Printer")
        val httpUri = with(ippUri) {
            val scheme = scheme.replace("ipp", "http")
            val port = if (port == -1) 631 else port
            URI.create("$scheme://$host:$port$rawPath")
        }
        httpUri.run {
            assertEquals("http://0:631/PDF%20Printer", toString())
            assertEquals("/PDF%20Printer", rawPath)
            assertEquals("/PDF Printer", path)
        }
    }

    @Test
    fun toHttpUriWithEncodedSpace_v2() {
        val ippUri = URI.create("ipp://0/PDF%20Printer")
        val httpUri = with(ippUri) {
            URI(scheme.replace("ipp", "http"), userInfo, host, if(port == -1) 631 else port, path, query, fragment)
        }
        httpUri.run {
            assertEquals("http://0:631/PDF%20Printer", toString())
            assertEquals("/PDF%20Printer", rawPath)
            assertEquals("/PDF Printer", path)
        }
    }
}