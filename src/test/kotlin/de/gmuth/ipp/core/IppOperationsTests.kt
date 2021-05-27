package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import kotlin.test.Test
import kotlin.test.assertEquals

class IppOperationsTests {

    @Test
    fun unknownOperationCode() {
        assertEquals(IppOperation.UnknownOperationCode, IppOperation.fromNumber(0))
    }

}