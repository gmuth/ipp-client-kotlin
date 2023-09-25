package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import kotlin.test.Test
import kotlin.test.assertFailsWith

class IppOperationsTests {

    @Test
    fun unknownOperationCodeFails() {
        assertFailsWith<IppException> {
            IppOperation.fromShort(0)
        }
    }

}