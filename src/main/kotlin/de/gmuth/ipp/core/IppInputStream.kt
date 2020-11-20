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
        var verbose: Boolean = false
        var check1setOfRegistration: Boolean = false
        var HP_BUG_WithLanguage_Workaround = true
    }

    // encoding for text and name attributes, RFC 8011 4.1.4.1
    private var attributesCharset: Charset? = null

    fun readMessage(message: IppMessage) {
        lateinit var currentGroup: IppAttributesGroup
        lateinit var currentAttribute: IppAttribute<*>
        message.version = IppVersion(read(), read())
        message.code = readShort()
        message.requestId = readInt()
        tagLoop@ do {
            val tag = readTag()
            when {
                tag.isEndTag() -> break@tagLoop
                tag.isDelimiterTag() -> {
                    ifVerbosePrintln("--- $tag ---")
                    currentGroup = message.ippAttributesGroup(tag)
                    continue@tagLoop
                }
            }
            val attribute = readAttribute(tag)
            ifVerbosePrintln("$attribute")
            if (attribute.name.isNotEmpty()) {
                currentGroup.put(attribute)
                currentAttribute = attribute
            } else { // name.isEmpty() -> 1setOf
                currentAttribute.additionalValue(attribute)
                if (check1setOfRegistration && IppRegistrationsSection2.attributeIs1setOf(currentAttribute.name) == false) {
                    println("WARN: '${currentAttribute.name}' is not registered as '1setOf'")
                }
            }
        } while (true)
    }

    private fun ifVerbosePrintln(string: String) {
        if (verbose) println(string)
    }

    private fun readTag(): IppTag = IppTag.fromByte(readByte())

    private fun readAttribute(tag: IppTag): IppAttribute<Any> {
        val name = readString()
        // RFC 8010 3.8. & RFC 3380 8
        if (tag.isOutOfBandTag() || tag == IppTag.EndCollection) {
            val valueBytes = readLengthAndValue()
            return if (valueBytes.isEmpty()) {
                IppAttribute(name, tag) // no value
            } else {
                // tolerate ipp spec violation, e.g. Xerox B210
                IppAttribute(name, tag, String(valueBytes))
            }
        } else {
            val value = try {
                readAttributeValue(tag)
            } catch (exception: Exception) {
                throw IppException("failed to read attribute value of '$name' ($tag)", exception)
            }
            // remember attributes-charset for name and text value decoding
            if (name == "attributes-charset") attributesCharset = value as Charset
            return IppAttribute(name, tag, value).apply { checkSyntax() }
        }
    }

    private fun readAttributeValue(tag: IppTag): Any {
        // RFC 8011 4.1.4.1
        fun readStringForTag() = readString(tag.selectCharset(attributesCharset))

        return when (tag) {

            // value class Boolean
            IppTag.Boolean -> {
                readExpectedValueLength(1)
                readByte() == 0x01.toByte()
            }

            // value class Int
            IppTag.Integer,
            IppTag.Enum -> {
                readExpectedValueLength(4)
                readInt()
            }

            // value class IntRange
            IppTag.RangeOfInteger -> {
                readExpectedValueLength(8)
                IntRange(
                        start = readInt(),
                        endInclusive = readInt()
                )
            }

            // value class IppResolution
            IppTag.Resolution -> {
                readExpectedValueLength(9)
                IppResolution(
                        x = readInt(),
                        y = readInt(),
                        unit = readByte().toInt()
                )
            }

            // value class Charset
            IppTag.Charset -> Charset.forName(readStringForTag())

            // value class URI
            IppTag.Uri -> URI.create(readStringForTag())

            // value class String with rfc 8011 3.9 and rfc 8011 4.1.4.1 attribute value encoding
            IppTag.Keyword,
            IppTag.UriScheme,
            IppTag.OctetString,
            IppTag.MimeMediaType,
            IppTag.MemberAttrName,
            IppTag.NaturalLanguage -> readStringForTag()

            // value class IppString
            IppTag.TextWithoutLanguage,
            IppTag.NameWithoutLanguage -> IppString(text = readStringForTag())

            IppTag.TextWithLanguage,
            IppTag.NameWithLanguage -> {
                val attributeValueLength = readShort().toInt()
                // HP M175nw: PrintJobOperation having a job-name with language & GetJobAttributes
                // Testcase: german macOS, print with application, read job attributes with ipptool
                val language = if (HP_BUG_WithLanguage_Workaround && attributeValueLength < 6) {
                    // attribute value length is missing, treat this as value length for language
                    String(readBytes(attributeValueLength), tag.selectCharset(attributesCharset))
                } else {
                    readStringForTag()
                }

                IppString(
                        language = language,
                        text = readStringForTag()
                )
            }

            // value class IppDateTime
            IppTag.DateTime -> {
                readExpectedValueLength(11)
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
                readExpectedValueLength(0)
                readCollection()
            }

            // unexpected state
            else -> throw IppException("decoding value for tag '$tag' not supported")
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
            when (attribute.tag) {
                IppTag.EndCollection -> {
                    currentMemberAttribute?.let { collection.add(it) }
                    break@memberLoop
                }
                IppTag.MemberAttrName -> {
                    currentMemberAttribute?.let { collection.add(it) }
                    currentMemberAttribute = null
                    memberName = attribute.value as String
                }
                else -> { // memberAttrValue
                    if (currentMemberAttribute == null) { // new memberduce
                        currentMemberAttribute = IppAttribute(memberName, attribute.tag, attribute.value)
                    } else {
                        currentMemberAttribute.additionalValue(attribute)
                    }
                }
            }
        }
        return collection
    }

    private fun readString(charset: Charset = Charsets.US_ASCII): String {
        val bytes = readLengthAndValue()
        return String(bytes, charset)
    }

    private fun readLengthAndValue(): ByteArray {
        val length = readShort().toInt()
        if (length > 1024) println("WARN: length $length of encoded value looks too large")
        return readBytes(length) // avoid Java-11-readNBytes(length) for backwards compatibility
    }

    private fun readBytes(length: Int) = ByteArray(length).apply { readFully(this) }

    private fun readExpectedValueLength(expected: Int) {
        val length = readShort().toInt()
        if (length != expected) {
            throw IppException("expected value length of $expected bytes but found $length")
        }
    }
}