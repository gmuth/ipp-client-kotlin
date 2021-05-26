package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IppStringTests {

    private val withoutLanguage = IppString("string-without-language")
    private val withLanguage = IppString("string-with-language", "en")

    @Test
    fun toStringWithoutLanguage() {
        assertEquals("string-without-language", withoutLanguage.toString())
    }

    @Test
    fun toStringWithLanguage() {
        assertEquals("string-with-language[en]", withLanguage.toString())
    }

    @Test
    fun toIppStringExtension() {
        assertEquals("some-text", "some-text".toIppString().text)
    }

}