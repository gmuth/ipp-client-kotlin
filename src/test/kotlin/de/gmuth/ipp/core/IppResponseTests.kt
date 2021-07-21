package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.File
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class IppResponseTests {

    private val ippResponse = IppResponse()

    @Test
    fun printJobResponse1() {
        with(IppResponse()) {
            read(File("src/test/resources/printJob.response"))
            assertTrue(isSuccessful())
            assertEquals(0, printerGroup.size)
            assertEquals(0, jobGroup.size)
            assertEquals(0, unsupportedGroup.size)
            assertEquals("successful-ok", codeDescription)
            assertEquals("not-infected", statusMessage.toString())
        }
    }

    @Test
    fun setStatus() {
        ippResponse.status = IppStatus.ClientErrorDocumentFormatNotSupported
        assertEquals(0x040A, ippResponse.code)
    }

    @Test
    fun invalidXeroxMediaColResponse() {
        assertFailsWith<IppException> {
            ippResponse.read(File("src/test/resources/invalidXeroxMediaCol.response"))
            ippResponse.logDetails()
            with(ippResponse.jobGroup) {
                assertEquals(598, getValue("job-id"))
                assertEquals(4, getValue("job-state")) // pending-held
                assertEquals(listOf("job-hold-until-specified"), getValues("job-state-reasons"))
                assertEquals(URI.create("ipp://xero.local./ipp/print/Job-598"), getValue("job-uri"))
            }
        }
    }

    @Test
    fun invalidHpNameWithLanguageResponse() {
        // IppInputStream solution: NameWithLanguage -> readShort().let { if (markSupported() && it < 6) reset() }
        assertFailsWith<IppException> {
            ippResponse.read(File("src/test/resources/invalidHpNameWithLanguage.response"))
            ippResponse.logDetails()
            with(ippResponse.jobGroup) {
                assertEquals(IppString("A4-blank.pdf", "de"), getValue("job-name"))
                assertEquals(993, getValue("job-id"))
                assertEquals(7, getValue("job-state")) // canceled
                assertEquals(listOf("none"), getValues("job-state-reasons"))
                assertEquals(URI.create("ipp://ColorJet.local/ipp/printer/0993"), getValue("job-uri"))
            }

        }
    }

}