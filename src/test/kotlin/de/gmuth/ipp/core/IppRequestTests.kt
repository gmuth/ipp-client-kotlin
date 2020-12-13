package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.log.Log
import java.io.File
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals

class IppRequestTests {

    companion object {
        val log = Log.getWriter("IppRequestTest", Log.Level.INFO)
    }

    @Test
    fun requestConstructor1() {
        val request = IppRequest()
        request.code = 5
        log.info { request.toString() }
        request.logDetails()
        assertEquals(1, request.requestId)
        assertEquals(IppVersion("1.1"), request.version)
        assertEquals(IppOperation.CreateJob, request.operation)
    }

    @Test
    fun requestConstructor2() {
        IppMessage.storeRawBytes = false
        val request = IppRequest(IppOperation.StartupPrinter)
        assertEquals(1, request.requestId)
        assertEquals(IppVersion("1.1"), request.version)
        assertEquals(IppOperation.StartupPrinter, request.operation)
        assertEquals(Charsets.UTF_8, request.operationGroup.getValue("attributes-charset"))
        assertEquals("en", request.operationGroup.getValue("attributes-natural-language"))
        assertEquals("Startup-Printer", request.codeDescription)
        assertEquals(72, request.encode().size)
        IppMessage.storeRawBytes = true
    }

    @Test
    fun validateJobRequest() {
        val request = IppRequest(
                IppOperation.ValidateJob, URI.create("ipp://localhost:8632/printers/laser"),
                1969, listOf("requested", "attributes"), "gmuth"
        )
        val requestEncoded = request.encode()
        log.info { request.toString() }
        request.logDetails()
        assertEquals(testResourceBytes("validateJob.request").toHex(), requestEncoded.toHex())
    }

    private fun testResourceBytes(filename: String) =
            File("src/test/resources/${filename}").readBytes()

}