package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2024 Gerhard Muth
 */

import de.gmuth.log.Logging
import java.net.URI
import java.util.logging.Logger.getLogger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IppResponseTests {

    init {
        Logging.configure()
    }

    private val logger = getLogger(javaClass.name)
    private val ippResponse = IppResponse()

    @Test
    fun printJobResponse() = ippResponse.run {
        readTestResource("/printJob.response")
        assertTrue(isSuccessful())
        assertEquals(0, printerGroup.size)
        assertEquals(0, jobGroup.size)
        assertEquals(0, unsupportedGroup.size)
        assertEquals("successful-ok", codeDescription)
        assertEquals("not-infected", statusMessage.toString())
    }

    @Test
    fun setStatus() = ippResponse.run {
        status = IppStatus.ClientErrorDocumentFormatNotSupported
        assertEquals(0x040A, code)
    }

    @Test
    fun invalidXeroxMediaColResponse() = ippResponse.run {
        readTestResource("/invalidXeroxMediaCol.response")
        log(logger)
        with(jobGroup) {
            assertEquals(598, getValue("job-id"))
            assertEquals(4, getValue("job-state")) // pending-held
            assertEquals(listOf("job-hold-until-specified"), getValues("job-state-reasons"))
            assertEquals(URI.create("ipp://xero.local./ipp/print/Job-598"), getValue("job-uri"))
        }
        with(unsupportedGroup) {
            assertEquals(0, getValue<IppCollection>("media-col").size)
        }
    }

    @Test
    fun invalidHpNameWithLanguageResponse() = ippResponse.run {
        // IppInputStream solution: first mark(2) then NameWithLanguage -> readShort().let { if (markSupported() && it < 6) reset() }
        // requestNaturalLanguage = "de" // triggers HP name with language bug
        readTestResource("/invalidHpNameWithLanguage.response")
        log(logger)
        with(jobGroup) {
            assertEquals(IppString("A4-blank.pdf", "de"), getValue("job-name"))
            assertEquals(993, getValue("job-id"))
            assertEquals(7, getValue("job-state")) // canceled
            assertEquals(listOf("none"), getValues("job-state-reasons"))
            assertEquals(URI.create("ipp://ColorJet.local/ipp/printer/0993"), getValue("job-uri"))
        }
    }

    @Test
    fun createResponse() {
        with(IppResponse(IppStatus.SuccessfulOk)) {
            assertTrue(isSuccessful())
        }
    }

}