package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.log.Logging
import java.io.File
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals

class IppRequestTests {

    companion object {
        val log = Logging.getLogger(Logging.LogLevel.INFO) {}
    }

    @Test
    fun requestConstructor1() {
        with(IppRequest()) {
            code = 5
            log.info { toString() }
            logDetails()
            assertEquals(1, requestId)
            assertEquals(IppVersion("1.1"), version)
            assertEquals(IppOperation.CreateJob, operation)
        }
    }

    @Test
    fun requestConstructor2() {
        IppMessage.log.logLevel = Logging.LogLevel.DEBUG
        IppMessage.saveRawBytes = !IppMessage.saveRawBytes
        val request = IppRequest(IppOperation.StartupPrinter)
        assertEquals(1, request.requestId)
        assertEquals(IppVersion("1.1"), request.version)
        assertEquals(IppOperation.StartupPrinter, request.operation)
        assertEquals(Charsets.UTF_8, request.operationGroup.getValue("attributes-charset"))
        assertEquals("en", request.operationGroup.getValue("attributes-natural-language"))
        assertEquals("Startup-Printer", request.codeDescription)
        val requestEncoded = request.encode()
        assertEquals(72, requestEncoded.size)
        IppMessage.saveRawBytes = !IppMessage.saveRawBytes
        IppMessage.log.logLevel = Logging.LogLevel.INFO
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
        val printJobRequest = File("src/test/resources/printJob.request").readBytes()
        assertEquals(printJobRequest.toHex(), requestEncoded.toHex())
    }

}