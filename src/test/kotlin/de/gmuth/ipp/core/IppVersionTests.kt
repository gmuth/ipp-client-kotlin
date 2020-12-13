package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import kotlin.test.Test
import kotlin.test.assertFailsWith

class IppVersionTests {

    @Test
    fun constructorFails() {
        assertFailsWith<IllegalArgumentException> { IppVersion("invalid version string") }
    }

}