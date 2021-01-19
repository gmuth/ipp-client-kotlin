package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class IppTagTests {

    @Test
    fun tagClassification() {
        assertFalse(IppTag.Printer.isOutOfBandTag())
        assertFalse(IppTag.Printer.isMemberAttrValue())
        assertFalse(IppTag.MemberAttrName.isMemberAttrValue())
    }

    @Test
    fun registeredSyntax() {
        assertEquals("name", IppTag.NameWithoutLanguage.registeredSyntax())
        assertEquals("text", IppTag.TextWithoutLanguage.registeredSyntax())
        assertEquals("keyword", IppTag.Keyword.registeredSyntax())
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