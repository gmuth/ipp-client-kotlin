package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IppResponseTests {

    @Test
    fun printJobResponse1() {
        val response = IppResponse().apply { read(File("src/test/resources/printJob.response")) }
        with(response) {
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
        IppResponse().apply { decode(File("src/test/resources/printJob.response").readBytes()) }
        IppMessage.storeRawBytes = !IppMessage.storeRawBytes
    }

}