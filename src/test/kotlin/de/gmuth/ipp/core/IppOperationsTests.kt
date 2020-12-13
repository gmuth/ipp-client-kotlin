package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IppOperationsTests {

    @Test
    fun fromShortFails() {
        assertFailsWith<IllegalArgumentException> { IppOperation.fromShort(0) }
    }

    @Test
    fun fromString() {
        assertEquals(IppOperation.PrintJob, IppOperation.fromString("Print-Job"))
    }

    @Test
    fun fromStringFails() {
        assertFailsWith<IllegalArgumentException> { IppOperation.fromString("invalid-operation-name") }
    }

}