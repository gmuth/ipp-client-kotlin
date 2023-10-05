package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import kotlin.test.*
import de.gmuth.ipp.core.IppTag.*
import java.net.URI

class IppTagTests {

    @Test
    fun validInteger() {
        assertFalse(Integer.valueHasValidClass("no-integer"))
        assertTrue(Integer.valueHasValidClass(1))
    }

    @Test
    fun validBoolean() {
        assertFalse(IppTag.Boolean.valueHasValidClass("no-boolean"))
        assertTrue(IppTag.Boolean.valueHasValidClass(true))
    }

    @Test
    fun validEnum() {
        assertFalse(IppTag.Enum.valueHasValidClass("no-enum"))
        assertTrue(IppTag.Enum.valueHasValidClass(1))
    }

    @Test
    fun validOctetString() {
        assertFalse(OctetString.valueHasValidClass(0))
        assertTrue(OctetString.valueHasValidClass("string"))
    }

    @Test
    fun validDateTime() {
        assertFalse(DateTime.valueHasValidClass(0))
        assertTrue(DateTime.valueHasValidClass(IppDateTime.now()))
    }

    @Test
    fun validResolution() {
        assertFalse(Resolution.valueHasValidClass(0))
        assertTrue(Resolution.valueHasValidClass(IppResolution(600)))
    }

    @Test
    fun validRangeOfInteger() {
        assertFalse(RangeOfInteger.valueHasValidClass(0))
        assertTrue(RangeOfInteger.valueHasValidClass(0..1))
    }

    @Test
    fun validBegCollection() {
        assertFalse(BegCollection.valueHasValidClass(0))
        assertTrue(BegCollection.valueHasValidClass(IppCollection()))
    }

    @Test
    fun validateTextWithLanguage() {
        assertFalse(TextWithLanguage.valueHasValidClass(0))
        assertTrue(TextWithLanguage.valueHasValidClass(IppString("")))
    }

    @Test
    fun validateNameWithLanguage() {
        assertFalse(NameWithLanguage.valueHasValidClass(0))
        assertTrue(NameWithLanguage.valueHasValidClass(IppString("")))
    }

    @Test
    fun validEndCollection() {
        assertTrue(EndCollection.valueHasValidClass(0))
        assertTrue(EndCollection.valueHasValidClass(""))
    }

    @Test
    fun validateTextWithoutLanguage() {
        assertFalse(TextWithoutLanguage.valueHasValidClass(0))
        assertTrue(TextWithoutLanguage.valueHasValidClass("string"))
        assertTrue(TextWithoutLanguage.valueHasValidClass(IppString("ipp-string")))
    }

    @Test
    fun validateNameWithoutLanguage() {
        assertFalse(NameWithoutLanguage.valueHasValidClass(0))
        assertTrue(NameWithoutLanguage.valueHasValidClass("string"))
        assertTrue(NameWithoutLanguage.valueHasValidClass(IppString("ipp-string")))
    }

    private fun validString(tag: IppTag) = tag.run {
        assertFalse(valueHasValidClass(0))
        assertTrue(valueHasValidClass("string"))
    }

    @Test
    fun validKeyword() =
        validString(Keyword)

    @Test
    fun validUri() {
        assertFalse(Uri.valueHasValidClass(0))
        assertTrue(Uri.valueHasValidClass(URI.create("ipp://0")))
    }

    @Test
    fun validUriScheme() =
        validString(UriScheme)

    @Test
    fun validCharset() {
        assertFalse(Charset.valueHasValidClass(0))
        assertTrue(Charset.valueHasValidClass(java.nio.charset.Charset.defaultCharset()))
    }

    @Test
    fun validNaturalLanguage() =
        validString(NaturalLanguage)

    @Test
    fun validMimeMediaType() =
        validString(MimeMediaType)

    @Test
    fun validMemberAttrName() =
        validString(MemberAttrName)

    @Test
    fun tagClassification() {
        assertFalse(Printer.isOutOfBandTag())
        assertFalse(Printer.isMemberAttrValue())
        assertFalse(MemberAttrName.isMemberAttrValue())
    }

    @Test
    fun registeredSyntax() {
        assertEquals("name", NameWithoutLanguage.registeredSyntax())
        assertEquals("text", TextWithoutLanguage.registeredSyntax())
        assertEquals("keyword", Keyword.registeredSyntax())
    }

    @Test
    fun fromString() {
        assertEquals(Uri, IppTag.fromString("uri"))
    }

    @Test
    fun fromStringFails() {
        assertFailsWith<IppException> { IppTag.fromString("invalid-tag-name") }
    }

    @Test
    fun fromByteFails() {
        assertFailsWith<IppException> { IppTag.fromByte(0x77) }
    }

}