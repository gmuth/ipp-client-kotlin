package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IppResponseTests {

    private val ippResponse = IppResponse()

    @Test
    fun printJobResponse1() {
        with(ippResponse) {
            read(File("src/test/resources/printJob.response"))
            assertTrue(isSuccessful())
            assertEquals(0, printerGroup.size)
            assertEquals(0, jobGroup.size)
            assertEquals("successful-ok", codeDescription)
            assertEquals("not-infected", statusMessage.toString())
        }
    }

    @Test
    fun printJobResponse2() {
        IppMessage.storeRawBytes = !IppMessage.storeRawBytes
        ippResponse.decode(File("src/test/resources/printJob.response").readBytes())
        IppMessage.storeRawBytes = !IppMessage.storeRawBytes
    }

    @Test
    fun setStatus() {
        ippResponse.status = IppStatus.ClientErrorDocumentFormatNotSupported
        assertEquals(0x040A, ippResponse.code)
    }

}