package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2024 Gerhard Muth
 */

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IppOperationsTests {

    @Test
    fun unknownOperationCodeFails() {
            val operation = IppOperation.fromInt(0)
            assertEquals(IppOperation.Unknown, operation)
    }

}