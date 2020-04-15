package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.iana.IppRegistrationsSection2
import java.io.DataInputStream
import java.io.InputStream
import java.net.URI
import java.nio.charset.Charset

class IppInputStream(inputStream: InputStream) : DataInputStream(inputStream) {

    companion object {
        var checkSyntax: Boolean = true
        var strict: Boolean = true
    }

    // encoding for text and name attributes, rfc 8011 4.1.4.1
    private var attributesCharset: Charset? = null

    private fun charsetForTag(tag: IppTag) =
            if (tag.useAttributesCharset()) attributesCharset ?: throw IppException("missing attributes-charset")
            else Charsets.US_ASCII

    fun readMessage(message: IppMessage) {
        with(message) {
            version = IppVersion(read(), read())
            code = readShort()
            requestId = readInt()
            lateinit var currentGroup: IppAttributesGroup
            lateinit var currentAttribute: IppAttribute<*>
            tagLoop@ while (true) {
                val tag = readTag()
                when {
                    tag == IppTag.End -> break@tagLoop
                    tag.isDelimiterTag() -> currentGroup = ippAttributesGroup(tag)
                    else -> {
                        val attribute = readAttribute(tag)
                        if (attribute.name.isNotEmpty()) {
                            currentGroup.put(attribute)
                            currentAttribute = attribute
                        } else { // name.isEmpty() -> 1setOf
                            currentAttribute.additionalValue(attribute)
                        }
                    }
                }
            }
        }
    }

    private fun readTag(): IppTag = IppTag.fromCode(readByte())

    private fun readAttribute(tag: IppTag): IppAttribute<*> {
        val name = readString(Charsets.US_ASCII)
        var value = try {
            readAttributeValue(tag)
        } catch (exception: Exception) {
            throw IppException("failed to read attribute value for '$name' ($tag)", exception)
        }

        if (checkSyntax) {
            IppRegistrationsSection2.checkSyntaxOfAttribute(name, tag)
        }

        // keep attributes-charset for name and text value decoding
        if (name == "attributes-charset" && tag == IppTag.Charset) {
            attributesCharset = Charset.forName(value as String)
        }

        return IppAttribute(name, tag, value)
    }

    private fun readAttributeValue(tag: IppTag): Any? = when (tag) {

        // out-of-band RFC 8010 3.8. & RFC 3380 8 -- endCollection has no value either
        IppTag.Unsupported_,
        IppTag.Unknown,
        IppTag.NoValue,
        IppTag.NotSettable,
        IppTag.DeleteAttribute,
        IppTag.AdminDefine,
        IppTag.EndCollection -> {
            assertValueLength(0)
            null
        }

        // value class Boolean
        IppTag.Boolean -> {
            assertValueLength(1)
            readByte() == 0x01.toByte()
        }

        // value class Int
        IppTag.Integer,
        IppTag.Enum -> {
            assertValueLength(4)
            readInt()
        }

        // value class IppIntegerRange
        IppTag.RangeOfInteger -> {
            assertValueLength(8)
            IppIntegerRange(
                    start = readInt(),
                    end = readInt()
            )
        }

        // value class IppResolution
        IppTag.Resolution -> {
            assertValueLength(9)
            IppResolution(
                    x = readInt(),
                    y = readInt(),
                    unit = readByte().toInt()
            )
        }

        // value class URI
        IppTag.Uri -> URI.create(readStringForTag(tag))

        // value class String with rfc 8011 3.9 and rfc 8011 4.1.4.1 attribute value encoding
        IppTag.OctetString,
        IppTag.Keyword,
        IppTag.UriScheme,
        IppTag.Charset,
        IppTag.NaturalLanguage,
        IppTag.MimeMediaType,
        IppTag.MemberAttrName -> readStringForTag(tag)

        // value class IppString
        IppTag.TextWithoutLanguage,
        IppTag.NameWithoutLanguage -> IppString(string = readStringForTag(tag))

        IppTag.TextWithLanguage,
        IppTag.NameWithLanguage -> {
            readShort() // ignore redundant value length
            IppString(
                    language = readStringForTag(tag),
                    string = readStringForTag(tag)
            )
        }

        // value class IppDateTime
        IppTag.DateTime -> {
            assertValueLength(11)
            IppDateTime(
                    year = readShort().toInt(),
                    month = read(),
                    day = read(),
                    hour = read(),
                    minutes = read(),
                    seconds = read(),
                    deciSeconds = read(),
                    directionFromUTC = readByte().toChar(),
                    hoursFromUTC = read(),
                    minutesFromUTC = read()
            )
        }

        //  value class IppCollection
        IppTag.BegCollection -> {
            assertValueLength(0)
            readCollection()
        }

        else -> {
            readLengthAndValue()
            String.format("<$tag-decoding-not-implemented>")
        }
    }

    private fun readCollection(): IppCollection {
        val collection = IppCollection()
        memberLoop@ while (true) {
            val memberAttributeName = readAttribute(readTag())
            if (memberAttributeName.tag == IppTag.EndCollection) break@memberLoop
            val memberAttributeValue = readAttribute(readTag())
            val member = IppAttribute(
                    memberAttributeName.value as String,
                    memberAttributeValue.tag,
                    memberAttributeValue.value
            )
            collection.add(member)
        }
        return collection
    }

    private fun readStringForTag(tag: IppTag): String {
        val charset = charsetForTag(tag)
        return readString(charset)
    }

    private fun readString(charset: Charset): String {
        val bytes = readLengthAndValue()
        return String(bytes, charset)
    }

    private fun readLengthAndValue(): ByteArray {
        val length = readShort().toInt()
        return readNBytes(length)
    }

    private fun assertValueLength(expected: Int) {
        val length = readShort().toInt()
        if (length != expected) {
            throw IppException("expected value length of $expected bytes but found $length")
        }
    }

}