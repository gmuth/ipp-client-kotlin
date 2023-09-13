package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import java.net.URI
import java.util.logging.Logger.getLogger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class IppRequestTests {

    val log = getLogger(javaClass.name)

    @Test
    fun requestConstructor1() {
        IppRequest().run {
            code = 5
            log.info { toString() }
            log(log)
            assertEquals(null, version)
            assertEquals(IppOperation.CreateJob, operation)
            createAttributesGroup(IppTag.Operation)
            assertFailsWith<IppException> { printerUri }
        }
    }

    @Test
    fun requestConstructor2() {
        val request = IppRequest(IppOperation.StartupPrinter, URI.create("ipp://foo"))
        assertEquals(1, request.requestId)
        assertEquals("1.1", request.version)
        assertEquals(IppOperation.StartupPrinter, request.operation)
        assertEquals(Charsets.UTF_8, request.attributesCharset)
        assertEquals("en", request.operationGroup.getValue("attributes-natural-language"))
        assertEquals("ipp://foo", request.printerUri.toString())
        assertEquals("Startup-Printer", request.codeDescription)
        val requestEncoded = request.encode()
        assertEquals(97, requestEncoded.size)
    }

    @Test
    fun printJobRequest() {
        val request = IppRequest(
            IppOperation.PrintJob, URI.create("ipp://printer"),
            0, listOf("one", "two"), "user"
        )
        request.documentInputStream = "pdl-content".byteInputStream()
        log.info { request.toString() }
        request.log(log)
        val requestEncoded = request.encode()
        log.info { "encoded ${requestEncoded.size} bytes" }
        val requestDecoded = IppRequest()
        requestDecoded.decode(requestEncoded)
        assertEquals("1.1", requestDecoded.version)
        assertEquals(IppOperation.PrintJob, requestDecoded.operation)
        assertEquals(1, requestDecoded.requestId)
        assertNotNull(requestDecoded.operationGroup)
        with(requestDecoded.operationGroup) {
            assertEquals(Charsets.UTF_8, getValue("attributes-charset"))
            assertEquals("en", getValue("attributes-natural-language"))
            assertEquals(URI.create("ipp://printer"), getValue("printer-uri"))
            assertEquals(0, getValue("job-id"))
            assertEquals(listOf("one", "two"), getValues("requested-attributes"))
            assertEquals("user".toIppString(), getValue("requesting-user-name"))
        }
        assertEquals("pdl-content", String(requestDecoded.documentInputStream!!.readBytes()))
    }

}