package de.gmuth.ipp.client

/**
 * Copyright (c) 2023 Gerhard Muth
 */

import org.junit.Test
import java.net.URI
import kotlin.test.assertEquals

class IppClientTests {
    val ippClient = IppClient()

    @Test
    fun toHttpUriWithEncodedSpace() {
        ippClient.toHttpUri(URI.create("ipp://0/PDF%20Printer")).run {
            assertEquals("http://0:631/PDF%20Printer", toString())
            assertEquals("/PDF%20Printer", rawPath)
            assertEquals("/PDF Printer", path)
        }
    }
}