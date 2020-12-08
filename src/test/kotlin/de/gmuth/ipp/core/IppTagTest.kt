package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class IppTagTest {

    @Test
    fun tagClassification() {
        assertTrue(IppTag.Subscription.isDelimiterTag())
        assertTrue(IppTag.NoValue.isOutOfBandTag())
        assertTrue(IppTag.End.isEndTag())
    }

    @Test
    fun registeredSyntax() {
        assertEquals("name", IppTag.NameWithoutLanguage.registeredSyntax())
        assertEquals("text", IppTag.TextWithoutLanguage.registeredSyntax())
    }

    @Test
    fun fromString() {
        assertEquals(IppTag.Uri, IppTag.fromString("uri"))
    }

    @Test
    fun fromStringFails() {
        assertFailsWith<IllegalArgumentException> { IppTag.fromString("invalid-tag-name") }
    }

    @Test
    fun fromByteFails() {
        assertFailsWith<IllegalArgumentException> { IppTag.fromByte(0x00) }
    }

}