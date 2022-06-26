package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import kotlin.test.*

class IppTagTests {

    @Test
    fun validateTextWithLanguage() {
        assertFalse(IppTag.TextWithoutLanguage.valueHasValidClass(0))
        assertTrue(IppTag.TextWithoutLanguage.valueHasValidClass("ipp-string".toIppString()))
    }

    @Test
    fun validateNameWithLanguage() {
        assertFalse(IppTag.NameWithoutLanguage.valueHasValidClass(0))
        assertTrue(IppTag.NameWithoutLanguage.valueHasValidClass("ipp-string".toIppString()))
    }

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
        assertFailsWith<NoSuchElementException> { IppTag.fromString("invalid-tag-name") }
    }

    @Test
    fun fromByteFails() {
        assertFailsWith<IppException> { IppTag.fromByte(0x77) }
    }

}