package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.iana.IppRegistrationsSection2
import java.io.DataInputStream
import java.io.InputStream
import java.net.URI
import java.nio.charset.Charset
import java.util.*

class IppInputStream(inputStream: InputStream) : DataInputStream(inputStream) {

    companion object {
        var verbose: Boolean = false
        var checkSyntax: Boolean = true
        var check1setOfRegistration: Boolean = false
    }

    // encoding for text and name attributes, rfc 8011 4.1.4.1
    private var attributesCharset: Charset? = null

    private fun charsetForTag(tag: IppTag) =
            if (tag.useAttributesCharset()) attributesCharset ?: throw IppException("missing attributes-charset")
            else Charsets.US_ASCII

    fun readMessage(message: IppMessage) {
        lateinit var currentGroup: IppAttributesGroup
        lateinit var currentAttribute: IppAttribute<*>
        message.version = IppVersion(read(), read())
        message.code = readShort()
        message.requestId = readInt()
        tagLoop@ while (true) {
            val tag = readTag()
            when {
                tag == IppTag.End -> break@tagLoop
                tag.isDelimiterTag() -> {
                    currentGroup = message.ippAttributesGroup(tag)
                    if (verbose) {
                        println("--- $tag ---")
                    }
                }
                else -> {
                    val attribute = readAttribute(tag)
                    if (verbose) {
                        println("<<< $attribute")
                    }
                    if (attribute.name.isNotEmpty()) {
                        currentGroup.put(attribute)
                        currentAttribute = attribute
                    } else { // name.isEmpty() -> 1setOf
                        currentAttribute.additionalValue(attribute)
                        if (check1setOfRegistration && IppRegistrationsSection2.attributeIs1setOf(currentAttribute.name) == false) {
                            println("WARN: '${currentAttribute.name}' is not registered as '1setOf'")
                        }
                    }
                }
            }
        }
    }

    private fun readTag(): IppTag = IppTag.fromByte(readByte())

    private fun readAttribute(tag: IppTag): IppAttribute<Any> {
        val name = readString(Charsets.US_ASCII)
        val value = try {
            readAttributeValue(tag)
        } catch (exception: Exception) {
            throw IppException("failed to read attribute value for '$name' ($tag)", exception)
        }

        if (checkSyntax) {
            IppRegistrationsSection2.checkSyntaxOfAttribute(name, tag)
        }

        // keep attributes-charset for name and text value decoding
        if (name == "attributes-charset" && tag == IppTag.Charset) {
            attributesCharset = value as Charset
        }

        return IppAttribute(name, tag, value)
    }

    private fun readAttributeValue(tag: IppTag): Any = when (tag) {

        // out-of-band RFC 8010 3.8. & RFC 3380 8 -- endCollection has no value either
        IppTag.Unsupported_,
        IppTag.Unknown,
        IppTag.NoValue,
        IppTag.NotSettable,
        IppTag.DeleteAttribute,
        IppTag.AdminDefine,
        IppTag.EndCollection -> {
            assertValueLength(0)
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

        // value class Charset
        IppTag.Charset -> Charset.forName(readStringForTag(tag))

        // value class Locale
        IppTag.NaturalLanguage -> Locale.forLanguageTag(readStringForTag(tag))

        // value class URI
        IppTag.Uri -> URI.create(readStringForTag(tag))

        // value class String with rfc 8011 3.9 and rfc 8011 4.1.4.1 attribute value encoding
        IppTag.OctetString,
        IppTag.Keyword,
        IppTag.UriScheme,
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
        lateinit var memberName: String
        var currentMemberAttribute: IppAttribute<Any>? = null
        val collection = IppCollection()
        memberLoop@ while (true) {
            val attribute = readAttribute(readTag())
            if (attribute.name.isNotEmpty()) {
                throw IppException("expected empty name but found '${attribute.name}'")
            }
            // if we have a member and the next attribute indicates a new member, add the current member to the collection
            if (currentMemberAttribute != null && attribute.tag in listOf(IppTag.MemberAttrName, IppTag.EndCollection)) {
                collection.add(currentMemberAttribute)
                currentMemberAttribute = null
            }
            when (attribute.tag) {
                IppTag.MemberAttrName -> memberName = attribute.value as String
                IppTag.EndCollection -> break@memberLoop
                else -> { // memberAttrValue
                    if (currentMemberAttribute == null) {
                        currentMemberAttribute = IppAttribute(memberName, attribute.tag, attribute.value)
                    } else {
                        currentMemberAttribute.additionalValue(attribute)
                    }
                }
            }
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