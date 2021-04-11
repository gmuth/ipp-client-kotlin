package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.io.ByteUtils.Companion.hexdump
import de.gmuth.ipp.iana.IppRegistrationsSection2
import de.gmuth.log.Logging
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.EOFException
import java.io.InputStream
import java.net.URI
import java.nio.charset.Charset

class IppInputStream(inputStream: InputStream) : DataInputStream(BufferedInputStream(inputStream)) {

    companion object {
        val log = Logging.getLogger {}
        var check1setOfRegistration: Boolean = false
    }

    // encoding for text and name attributes, RFC 8011 4.1.4.1
    internal var attributesCharset: Charset? = null

    fun readMessage(message: IppMessage) {
        with(message) {
            version = IppVersion(read(), read())
            log.trace { "version = $version" }

            code = readShort()
            log.trace { "code = $code ($codeDescription)" }

            requestId = readInt()
            log.trace { "requestId = $requestId" }
        }

        lateinit var currentGroup: IppAttributesGroup
        lateinit var currentAttribute: IppAttribute<*>
        do {
            val tag = readTag()
            when {
                tag.isGroupTag() -> {
                    // bugfix for macOS-CUPS-PrintJob-Operation (sends "document-format" multiple times): deny attribute replacement
                    currentGroup = message.createAttributesGroup(tag, tag != IppTag.Operation)
                }
                tag.isValueTag() -> {
                    val attribute = readAttribute(tag)
                    log.trace { "$attribute" }
                    if (attribute.name.isNotEmpty()) {
                        currentGroup.put(attribute)
                        currentAttribute = attribute
                    } else { // name.isEmpty() -> 1setOf
                        currentAttribute.additionalValue(attribute)
                        if (check1setOfRegistration && IppRegistrationsSection2.attributeIs1setOf(currentAttribute.name) == false) {
                            log.warn { "'${currentAttribute.name}' is not registered as '1setOf'" }
                        }
                    }
                }
            }
        } while (tag != IppTag.End)
    }

    internal fun readTag() =
            IppTag.fromByte(readByte()).apply {
                if (isDelimiterTag()) log.trace { "--- $this ---" }
            }

    internal fun readAttribute(tag: IppTag): IppAttribute<Any> {
        val name = readString()
        // RFC 8010 3.8. & RFC 3380 8
        if (tag.isOutOfBandTag() || tag == IppTag.EndCollection) {
            val valueBytes = readLengthAndValue()
            return if (valueBytes.isEmpty()) {
                IppAttribute(name, tag) // no value
            } else {
                IppAttribute(name, tag, valueBytes)
            }
        } else {
            val value = try {
                readAttributeValue(tag)
            } catch (exception: Exception) {
                if (exception !is EOFException) {
                    val remainingBytes = readAllBytes()
                    hexdump(remainingBytes) { line -> log.debug { line } }
                    log.debug { String(remainingBytes) }
                }
                throw IppException("failed to read attribute value of '$name' ($tag)", exception)
            }
            // remember attributes-charset for name and text value decoding
            if (name == "attributes-charset") attributesCharset = value as Charset
            return IppAttribute(name, tag, value)
        }
    }

    internal fun readAttributeValue(tag: IppTag): Any =
            when (tag) {

                // value class Boolean
                IppTag.Boolean -> {
                    readExpectedValueLength(1)
                    readBoolean()
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
                IppTag.Charset -> Charset.forName(readString())

                // value class URI
                IppTag.Uri -> URI.create(readString())

                // value class String with rfc 8011 3.9 and rfc 8011 4.1.4.1 attribute value encoding
                IppTag.Keyword,
                IppTag.UriScheme,
                IppTag.OctetString,
                IppTag.MimeMediaType,
                IppTag.MemberAttrName,
                IppTag.NaturalLanguage -> readString()

                // value class IppString
                IppTag.TextWithoutLanguage,
                IppTag.NameWithoutLanguage -> IppString(text = readString(attributesCharset!!))

                IppTag.TextWithLanguage,
                IppTag.NameWithLanguage -> {
                    val attributeValueLength = readShort().toInt()
                    // HP M175nw: PrintJobOperation having a job-name with language & GetJobAttributes
                    // Testcase: german macOS, print with application, read job attributes
                    val language = if (attributeValueLength < 6) {
                        // attribute value length is missing, treat this as value length for language
                        String(readBytes(attributeValueLength), attributesCharset!!)
                    } else {
                        readString(attributesCharset!!)
                    }
                    IppString(
                            language = language,
                            text = readString(attributesCharset!!)
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
                    mark(2)
                    val length = readShort().toInt()
                    if (length == 0) { // expected value length
                        readCollection()
                    } else {
                        // Xerox B210: support invalid ipp response ('media-col' has no members)
                        log.warn { "unexpected value length $length, trying to recover at tag level..." }
                        reset()
                        IppCollection()
                    }
                }

                else -> throw IllegalArgumentException("tag '$tag'")
            }

    private fun readCollection() =
            IppCollection().apply {
                var memberAttribute: IppAttribute<Any>? = null
                do {
                    val attribute = readAttribute(readTag())
                    if (memberAttribute != null && attribute.tag in listOf(IppTag.EndCollection, IppTag.MemberAttrName)) {
                        add(memberAttribute)
                    }
                    when {
                        attribute.tag.isMemberAttrName() -> {
                            val memberName = attribute.value as String
                            val firstValue = readAttribute(readTag())
                            memberAttribute = IppAttribute(memberName, firstValue.tag, firstValue.value)
                        }
                        attribute.tag.isMemberAttrValue() -> {
                            memberAttribute!!.additionalValue(attribute)
                        }
                    }
                } while (attribute.tag != IppTag.EndCollection)
            }

    // RFC 8011 4.1.4.1 -> use attributes-charset
    private fun readString(charset: Charset = Charsets.US_ASCII) =
            String(readLengthAndValue(), charset)

    private fun readLengthAndValue() =
            readBytes(readShort().toInt())

    // avoid Java-11-readNBytes(length) for backwards compatibility
    private fun readBytes(length: Int) =
            ByteArray(length).apply { readFully(this) }

    private fun readExpectedValueLength(expected: Int) {
        val length = readShort().toInt()
        if (length != expected) throw IppException("expected value length of $expected bytes but found $length")
    }

}