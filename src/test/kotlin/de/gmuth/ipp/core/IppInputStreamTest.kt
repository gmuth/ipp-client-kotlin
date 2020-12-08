package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.log.Log
import java.io.ByteArrayInputStream
import java.io.EOFException
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class IppInputStreamTest {

    companion object {
        val log = Log.getWriter("IppInputStreamTest", Log.Level.INFO)
    }

    private val message = object : IppMessage() {
        override val codeDescription: String
            get() = "codeDescription"
    }

    @Test
    fun readAttributeValueBooleanFalse() {
        val encoded = "00 01 00"
        assertEquals(false, encoded.readAttributeValue(IppTag.Boolean))
    }

    @Test
    fun readAttributeValueBooleanTrue() {
        val encoded = "00 01 01"
        assertEquals(true, encoded.readAttributeValue(IppTag.Boolean))
    }

    @Test
    fun readAttributeValueInteger() {
        val encoded = "00 04 12 34 56 78"
        assertEquals(0x12345678, encoded.readAttributeValue(IppTag.Integer))
    }

    @Test
    fun readAttributeValueEnum() {
        val encoded = "00 04 01 02 03 04"
        assertEquals(0x01020304, encoded.readAttributeValue(IppTag.Enum))
    }

    @Test
    fun readAttributeValueRangeOfInteger() {
        val encoded = "00 08 00 00 00 10 00 00 04 00"
        assertEquals(16..1024, encoded.readAttributeValue(IppTag.RangeOfInteger))
    }

    @Test
    fun readAttributeValueResolution() {
        val encoded = "00 09 00 00 02 58 00 00 02 58 03"
        assertEquals(IppResolution(600), encoded.readAttributeValue(IppTag.Resolution))
    }

    @Test
    fun readAttributeValueCharset() {
        val encoded = "00 08 75 73 2D 61 73 63 69 69"
        assertEquals(Charsets.US_ASCII, encoded.readAttributeValue(IppTag.Charset))
    }

    @Test
    fun readAttributeValueUri() {
        val encoded = "00 0C 69 70 70 3A 2F 2F 6A 6F 65 6C 6C 65"
        assertEquals(URI.create("ipp://joelle"), encoded.readAttributeValue(IppTag.Uri))
    }

    @Test
    fun readAttributeValueKeyword() {
        val encoded = "00 08 61 4B 65 79 77 6F 72 64"
        assertEquals("aKeyword", encoded.readAttributeValue(IppTag.Keyword))
    }

    @Test
    fun readAttributeValueUriScheme() {
        val encoded = "00 04 69 70 70 73"
        assertEquals("ipps", encoded.readAttributeValue(IppTag.UriScheme))
    }

    @Test
    fun readAttributeValueOctetString() {
        val encoded = "00 0D 61 6E 4F 63 74 65 74 53 74 72 69 6E 67"
        assertEquals("anOctetString", encoded.readAttributeValue(IppTag.OctetString))
    }

    @Test
    fun readAttributeValueMimeMediaType() {
        val encoded = "00 0F 61 70 70 6C 69 63 61 74 69 6F 6E 2F 70 64 66"
        assertEquals("application/pdf", encoded.readAttributeValue(IppTag.MimeMediaType))
    }

    @Test
    fun readAttributeValueMemberAttrName() {
        val encoded = "00 12 61 2D 6D 65 6D 62 65 72 2D 61 74 74 72 2D 6E 61 6D 65"
        assertEquals("a-member-attr-name", encoded.readAttributeValue(IppTag.MemberAttrName))
    }

    @Test
    fun readAttributeValueNaturalLanguage() {
        val encoded = "00 05 65 6E 2D 75 73"
        assertEquals("en-us", encoded.readAttributeValue(IppTag.NaturalLanguage))
    }

    @Test
    fun readAttributeValueTextWithoutLanguage() {
        val encoded = "00 14 61 54 65 78 74 57 69 74 68 6F 75 74 4C 61 6E 67 75 61 67 65"
        assertEquals(IppString("aTextWithoutLanguage"), encoded.readAttributeValue(IppTag.TextWithoutLanguage))
    }

    @Test
    fun readAttributeValueNameWithoutLanguage() {
        val encoded = "00 14 61 4E 61 6D 65 57 69 74 68 6F 75 74 4C 61 6E 67 75 61 67 65"
        assertEquals(IppString("aNameWithoutLanguage"), encoded.readAttributeValue(IppTag.NameWithoutLanguage))
    }

    @Test
    fun readAttributeValueTextWithLanguage() {
        val encoded = "00 17 00 02 65 6E 00 11 61 54 65 78 74 57 69 74 68 4C 61 6E 67 75 61 67 65"
        assertEquals(IppString("aTextWithLanguage", "en"), encoded.readAttributeValue(IppTag.TextWithLanguage))
    }

    @Test
    fun readAttributeValueNameWithLanguage() {
        val encoded = "00 17 00 02 64 65 00 11 65 69 6E 4E 61 6D 65 4D 69 74 53 70 72 61 63 68 65"
        assertEquals(IppString("einNameMitSprache", "de"), encoded.readAttributeValue(IppTag.NameWithLanguage))
    }

    fun readAttributeValueNameWithLanguage_HP_BugFails() {
        //IppInputStream.HP_BUG_WithLanguage_Workaround = false
        // value length 0x0017 is missing
        val encoded = "00 02 64 65 00 11 65 69 6E 4E 61 6D 65 4D 69 74 53 70 72 61 63 68 65"
        assertFailsWith<EOFException> { encoded.readAttributeValue(IppTag.NameWithLanguage) }
    }

    @Test
    fun readAttributeValueNameWithLanguage_HP_BugWorkaround() {
        // value length 0x0017 is missing
        val encoded = "00 02 64 65 00 11 65 69 6E 4E 61 6D 65 4D 69 74 53 70 72 61 63 68 65"
        assertEquals(IppString("einNameMitSprache", "de"), encoded.readAttributeValue(IppTag.NameWithLanguage))
    }

    @Test
    fun readAttributeValueDateTime() {
        val encoded = "00 0B 07 D7 03 0F 02 0F 25 00 2B 01 00"
        assertEquals(IppDateTime(2007, 3, 15, 2, 15, 37, 0, '+', 1, 0), encoded.readAttributeValue(IppTag.DateTime))
    }

    @Test
    fun readAttributeValueCollection() {
        val encoded = "00 00 4A 00 00 00 03 66 6F 6F 44 00 00 00 01 61 44 00 00 00 01 62 37 00 00 00 00"
        assertEquals(IppCollection(IppAttribute("foo", IppTag.Keyword, "a", "b")), encoded.readAttributeValue(IppTag.BegCollection))
    }

    @Test
    fun readAttributeValueUnknownFails() {
        val encoded = "00 00 12"
        assertFailsWith<IllegalArgumentException> { encoded.readAttributeValue(IppTag.Unknown) }
    }

    @Test
    fun readMessage() {
        val encoded = "02 01 00 0B 00 00 00 08 01 47 00 12 61 74 74 72 69 62 75 74 65 73 2D 63 68 61 72 73 65 74 00 05 75 74 66 2D 38 02 22 00 01 31 00 01 01 22 00 00 00 01 00 13 00 01 30 00 00 03"

        IppInputStream.log.level = Log.Level.TRACE
        encoded.toIppInputStream().readMessage(message)
        IppInputStream.log.level = Log.Level.TRACE
        with(message) {
            assertEquals(IppVersion("2.1"), version)
            assertEquals(IppOperation.GetPrinterAttributes.code, code)
            assertEquals(8, requestId)
            with(getSingleAttributesGroup(IppTag.Job)) {
                assertEquals(2, size)
                with(get("1")!!) {
                    assertEquals(IppTag.Boolean, this.tag)
                    assertEquals(listOf(true, false), values as List<*>)
                }
                with(get("0")!!) {
                    assertEquals(IppTag.NoValue, this.tag)
                }
            }
        }
    }

    @Test
    fun readMessageReadAttributeFails() {
        val encoded = "00 03 66 6F 6F 00 03 62 61 72"
        assertFailsWith<IppException> { encoded.toIppInputStream().readAttribute(IppTag.Integer) }
    }

    @Test
    fun readUnsupportedAttribute() {
        val encoded = "00 01 66 00 01 40"
        val attribute = encoded.toIppInputStream().readAttribute(IppTag.Unsupported_)
        assertTrue(attribute.value is ByteArray)
        with(attribute.value as ByteArray) { assertEquals(1, size) }
    }

    @Test
    fun coverCheck1setOf_true() {
        IppInputStream.check1setOfRegistration = true
        // jobGroup with job-state 1setOf boolean -> 'job-state' is not registered as '1setOf'
        val encoded = "01 01 00 02 00 00 00 01 02 22 00 09 6A 6F 62 2D 73 74 61 74 65 00 01 01 22 00 00 00 01 00 03"
        encoded.toIppInputStream().readMessage(message)
        assertEquals(1, message.attributesGroups.size)
    }

    @Test
    fun coverCheck1setOf_false1() {
        IppInputStream.check1setOfRegistration = true
        // jobGroup with job-state-reasons 1setOf boolean -> false
        val encoded = "01 01 00 02 00 00 00 01 02 22 00 11 6A 6F 62 2D 73 74 61 74 65 2D 72 65 61 73 6F 6E 73 00 01 01 22 00 00 00 01 00 03"
        encoded.toIppInputStream().readMessage(message)
        assertEquals(1, message.attributesGroups.size)
    }

    @Test
    fun coverCheck1setOf_false2() {
        IppInputStream.check1setOfRegistration = false
        // jobGroup with job-state-reasons 1setOf boolean -> false
        val encoded = "01 01 00 02 00 00 00 01 02 22 00 11 6A 6F 62 2D 73 74 61 74 65 2D 72 65 61 73 6F 6E 73 00 01 01 22 00 00 00 01 00 03"
        encoded.toIppInputStream().readMessage(message)
        assertEquals(1, message.attributesGroups.size)
    }
}

// hex utility extensions

fun String.toByteArray() =
        ByteArray((length + 1) / 3) { substring(3 * it, 3 * it + 2).toInt(16).toByte() }

fun String.toIppInputStream() =
        IppInputStream(ByteArrayInputStream(toByteArray())).apply { attributesCharset = Charsets.US_ASCII }

fun String.readAttributeValue(tag: IppTag) =
        toIppInputStream().readAttributeValue(tag)