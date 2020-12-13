package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class IppDateTimeTests {

    private val ippDateTime = IppDateTime(2020, 12, 13, 11, 22, 33, 4, '+', 1, 0)

    @Test
    fun toZonedDateTime() {
        assertEquals("2020-12-13T11:22:33.400+01:00", ippDateTime.toZonedDateTime().toString())
    }

    @Test
    fun constructorZonedDateTime() {
        assertEquals(ippDateTime, IppDateTime(ZonedDateTime.parse("2020-12-13T11:22:33.400+01:00")))
    }

}