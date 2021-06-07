package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppResolution.Unit.DPC
import kotlin.test.Test
import kotlin.test.assertEquals

class IppResolutionTests {

    @Test
    fun toStringTestDpc() {
        assertEquals("600 dpc", IppResolution(600, 600, DPC).toString())
    }

    @Test
    fun toStringTestXdpi() {
        assertEquals("1200x600 dpi", IppResolution(1200, 600).toString())
    }

}