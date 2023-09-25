package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IppStringTests {

    private val withoutLanguage = IppString("string-without-language")
    private val withLanguage = IppString("string-with-language", "en")

    @Test
    fun toStringWithoutLanguage() {
        assertEquals("string-without-language", withoutLanguage.toString())
    }

    @Test
    fun toStringWithLanguage() {
        assertEquals("[en] string-with-language", withLanguage.toString())
    }

    @Test
    fun toIppStringExtension() {
        assertEquals("string-without-language", withoutLanguage.text)
        assertNull(withoutLanguage.language)
    }

    @Test
    fun toIppStringExtensionWithLanguage() {
        assertEquals("string-with-language", withLanguage.text)
        assertEquals("en", withLanguage.language)
    }

}