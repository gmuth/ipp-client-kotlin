package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppResolution.Unit.DPI
import de.gmuth.log.Logging
import java.io.ByteArrayOutputStream
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IppOutputStreamTest {

    companion object {
        val log = Logging.getLogger(Logging.LogLevel.INFO) {}
    }

    private val byteArrayOutputStream = ByteArrayOutputStream()
    private val ippOutputStream = IppOutputStream(byteArrayOutputStream).apply { attributesCharset = Charsets.US_ASCII }

    private val message = object : IppMessage() {
        override val codeDescription: String
            get() = "codeDescription"

        init {
            createAttributesGroup(IppTag.Operation).attribute("attributes-charset", IppTag.Charset, Charsets.UTF_8)
        }
    }

    @Test
    fun writeVersion() {
        ippOutputStream.writeVersion("2.1")
        assertEquals("02 01", byteArrayOutputStream.toHex())
    }

    @Test
    fun writeAttributeValueBooleanFalse() {
        ippOutputStream.writeAttributeValue(IppTag.Boolean, false)
        assertEquals("00 01 00", byteArrayOutputStream.toHex())
    }

    @Test
    fun writeAttributeValueBooleanTrue() {
        ippOutputStream.writeAttributeValue(IppTag.Boolean, true)
        assertEquals("00 01 01", byteArrayOutputStream.toHex())
    }

    @Test
    fun writeAttributeValueInteger() {
        ippOutputStream.writeAttributeValue(IppTag.Integer, 0x12345678)
        assertEquals("00 04 12 34 56 78", byteArrayOutputStream.toHex())
    }

    @Test
    fun writeAttributeValueEnum() {
        ippOutputStream.writeAttributeValue(IppTag.Enum, 0x01020304)
        assertEquals("00 04 01 02 03 04", byteArrayOutputStream.toHex())
    }

    @Test
    fun writeAttributeValueRangeOfInteger() {
        ippOutputStream.writeAttributeValue(IppTag.RangeOfInteger, 16..1024)
        assertEquals("00 08 00 00 00 10 00 00 04 00", byteArrayOutputStream.toHex())
    }

    @Test
    fun writeAttributeValueResolution() {
        ippOutputStream.writeAttributeValue(IppTag.Resolution, IppResolution(600, DPI))
        assertEquals("00 09 00 00 02 58 00 00 02 58 03", byteArrayOutputStream.toHex())
    }

    @Test
    fun writeAttributeValueCharset() {
        ippOutputStream.writeAttributeValue(IppTag.Charset, Charsets.US_ASCII)
        assertEquals("00 08 75 73 2D 61 73 63 69 69", byteArrayOutputStream.toHex())
    }

    @Test
    fun writeAttributeValueUri() {
        ippOutputStream.writeAttributeValue(IppTag.Uri, URI.create("ipp://joelle"))
        assertEquals("00 0C 69 70 70 3A 2F 2F 6A 6F 65 6C 6C 65", byteArrayOutputStream.toHex())
    }

    @Test
    fun writeAttributeValueKeyword() {
        ippOutputStream.writeAttributeValue(IppTag.Keyword, "aKeyword")
        assertEquals("00 08 61 4B 65 79 77 6F 72 64", byteArrayOutputStream.toHex())
    }

    @Test
    fun writeAttributeValueUriScheme() {
        ippOutputStream.writeAttributeValue(IppTag.UriScheme, "ipps")
        assertEquals("00 04 69 70 70 73", byteArrayOutputStream.toHex())
    }

    @Test
    fun writeAttributeValueOctetString() {
        ippOutputStream.writeAttributeValue(IppTag.OctetString, "anOctetString")
        assertEquals("00 0D 61 6E 4F 63 74 65 74 53 74 72 69 6E 67", byteArrayOutputStream.toHex())
    }

    @Test
    fun writeAttributeValueMimeMediaType() {
        ippOutputStream.writeAttributeValue(IppTag.MimeMediaType, "application/pdf")
        assertEquals("00 0F 61 70 70 6C 69 63 61 74 69 6F 6E 2F 70 64 66", byteArrayOutputStream.toHex())
    }

    @Test
    fun writeAttributeValueMemberAttrName() {
        ippOutputStream.writeAttributeValue(IppTag.MemberAttrName, "a-member-attr-name")
        assertEquals("00 12 61 2D 6D 65 6D 62 65 72 2D 61 74 74 72 2D 6E 61 6D 65", byteArrayOutputStream.toHex())
    }

    @Test
    fun writeAttributeValueNaturalLanguage() {
        ippOutputStream.writeAttributeValue(IppTag.NaturalLanguage, "en-us")
        assertEquals("00 05 65 6E 2D 75 73", byteArrayOutputStream.toHex())
    }

    @Test
    fun writeAttributeValueTextWithoutLanguage() {
        ippOutputStream.writeAttributeValue(IppTag.TextWithoutLanguage, IppString("aTextWithoutLanguage"))
        assertEquals("00 14 61 54 65 78 74 57 69 74 68 6F 75 74 4C 61 6E 67 75 61 67 65", byteArrayOutputStream.toHex())
    }

    @Test
    fun writeAttributeValueNameWithoutLanguage() {
        ippOutputStream.writeAttributeValue(IppTag.NameWithoutLanguage, IppString("aNameWithoutLanguage"))
        assertEquals("00 14 61 4E 61 6D 65 57 69 74 68 6F 75 74 4C 61 6E 67 75 61 67 65", byteArrayOutputStream.toHex())
    }

    @Test
    fun writeAttributeValueNameWithoutLanguageFails() {
        assertFailsWith<ClassCastException> { ippOutputStream.writeAttributeValue(IppTag.NameWithoutLanguage, 0) }
    }

    @Test
    fun writeAttributeValueTextWithLanguage() {
        ippOutputStream.writeAttributeValue(IppTag.TextWithLanguage, IppString("aTextWithLanguage", "en"))
        assertEquals("00 17 00 02 65 6E 00 11 61 54 65 78 74 57 69 74 68 4C 61 6E 67 75 61 67 65", byteArrayOutputStream.toHex())
    }

    @Test
    fun writeAttributeValueNameWithLanguage() {
        ippOutputStream.writeAttributeValue(IppTag.NameWithLanguage, IppString("einNameMitSprache", "de"))
        assertEquals("00 17 00 02 64 65 00 11 65 69 6E 4E 61 6D 65 4D 69 74 53 70 72 61 63 68 65", byteArrayOutputStream.toHex())
    }

    @Test
    fun writeAttributeValueNameWithLanguageFails() {
        assertFailsWith<IppException> { ippOutputStream.writeAttributeValue(IppTag.NameWithLanguage, "text-without-language".toIppString()) }
    }

    @Test
    fun writeAttributeValueDateTime() {
        ippOutputStream.writeAttributeValue(IppTag.DateTime, IppDateTime(2007, 3, 15, 2, 15, 37, 0, '+', 1, 0))
        assertEquals("00 0B 07 D7 03 0F 02 0F 25 00 2B 01 00", byteArrayOutputStream.toHex())
    }

    @Test
    fun writeAttributeValueCollection() {
        ippOutputStream.writeAttributeValue(IppTag.BegCollection, IppCollection(IppAttribute("foo", IppTag.Keyword, "a", "b")))
        assertEquals("00 00 4A 00 00 00 03 66 6F 6F 44 00 00 00 01 61 44 00 00 00 01 62 37 00 00 00 00", byteArrayOutputStream.toHex())
    }

    @Test
    fun writeAttributeValueUnknownFails() {
        assertFailsWith<IppException> { ippOutputStream.writeAttributeValue(IppTag.Unknown, "unknownValue") }
    }

    @Test
    fun writeMessage() {
        with(message) {
            version = "2.1"
            code = IppOperation.GetPrinterAttributes.code
            requestId = 8
            with(createAttributesGroup(IppTag.Job)) {
                attribute("1", IppTag.Boolean, true, false) // cover 1setOf
                attribute("0", IppTag.NoValue) // cover OutOfBand
            }
        }
        IppOutputStream.log.logLevel = Logging.LogLevel.TRACE
        ippOutputStream.writeMessage(message)
        IppOutputStream.log.logLevel = Logging.LogLevel.WARN

        assertEquals(
                "02 01 00 0B 00 00 00 08 01 47 00 12 61 74 74 72 69 62 75 74 65 73 2D 63 68 61 72 73 65 74 00 05 75 74 66 2D 38 02 22 00 01 31 00 01 01 22 00 00 00 01 00 13 00 01 30 00 00 03",
                byteArrayOutputStream.toHex()
        )
    }

    @Test
    fun writeMessageMissingVersion() {
        val exception = assertFailsWith<IppException> { ippOutputStream.writeMessage(message) }
        assertEquals(exception.message, "missing version")
    }

    @Test
    fun writeMessageMissingCode() {
        message.version = "1.1"
        val exception = assertFailsWith<IppException> { ippOutputStream.writeMessage(message) }
        assertEquals(exception.message, "missing operation or status code")
    }

    @Test
    fun writeMessageMissingRequestId() {
        message.version = "1.1"
        message.code = 0
        val exception = assertFailsWith<IppException> { ippOutputStream.writeMessage(message) }
        assertEquals("missing requestId", exception.message)
    }

    @Test
    fun writeMessageFails() {
        with(message) {
            version = "2.1"
            code = IppOperation.GetPrinterAttributes.code
            requestId = 8
            operationGroup.attribute("foo", IppTag.TextWithLanguage, IppString("text-without-language"))
        }
        assertFailsWith<IppException> { ippOutputStream.writeMessage(message) }
    }

}

// hex utility extensions

fun ByteArray.toHex() = joinToString(" ") { "%02X".format(it) }

fun ByteArrayOutputStream.toHex() = toByteArray().toHex()