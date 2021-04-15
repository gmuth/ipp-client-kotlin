package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.io.ByteArraySavingBufferedInputStream
import de.gmuth.log.Logging
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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
        assertEquals("en-us", request.operationGroup.getValue("attributes-natural-language"))
        assertEquals("Startup-Printer", request.codeDescription)
        val requestEncoded = request.encode()
        assertEquals(75, requestEncoded.size)
        IppMessage.saveRawBytes = !IppMessage.saveRawBytes
        IppMessage.log.logLevel = Logging.LogLevel.INFO
    }

    @Test
    fun printJobRequest() {
        ByteArraySavingBufferedInputStream.log.logLevel = Logging.LogLevel.INFO
        IppInputStream.log.logLevel = Logging.LogLevel.INFO
        IppOutputStream.log.logLevel = Logging.LogLevel.INFO
        IppMessage.log.logLevel = Logging.LogLevel.INFO

        val request = IppRequest(
                IppOperation.PrintJob, URI.create("ipp://printer"),
                0, listOf("one", "two"), "user"
        )
        request.documentInputStream = "pdl-content".byteInputStream()
        log.info { request.toString() }
        request.logDetails()
        val requestEncoded = request.encode()
        log.info { "encoded ${requestEncoded.size} bytes" }
        val requestDecoded = IppRequest()
        IppMessage.saveRawBytes = true
        requestDecoded.decode(requestEncoded)
        assertEquals("1.1", requestDecoded.version.toString())
        assertEquals(IppOperation.PrintJob, requestDecoded.operation)
        assertEquals(1, requestDecoded.requestId)
        assertNotNull(requestDecoded.operationGroup)
        with(requestDecoded.operationGroup) {
            assertEquals(Charsets.UTF_8, getValue("attributes-charset"))
            assertEquals("en-us", getValue("attributes-natural-language"))
            assertEquals(URI.create("ipp://printer"), getValue("printer-uri"))
            assertEquals(0, getValue("job-id"))
            assertEquals(listOf("one", "two"), getValues("requested-attributes"))
            assertEquals("user".toIppString(), getValue("requesting-user-name"))
        }
        assertEquals("pdl-content", String(requestDecoded.documentInputStream!!.readAllBytes()))
    }

}