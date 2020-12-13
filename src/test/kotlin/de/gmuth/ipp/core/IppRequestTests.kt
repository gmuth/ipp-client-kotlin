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
        val log = Log.getWriter("IppRequestTests", Log.Level.INFO)
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
    fun printJobRequest() {
        val request = IppRequest(
                IppOperation.PrintJob, URI.create("ipp://printer"),
                0, listOf("one", "two"), "user"
        )
        request.documentInputStream = "content".byteInputStream()
        val requestEncoded = request.encode()
        log.info { request.toString() }
        request.logDetails()
        val printJobBytes = File("src/test/resources/printJob.request").readBytes()
        assertEquals(printJobBytes.toHex(), requestEncoded.toHex())
    }

}