package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2022 Gerhard Muth
 */

import de.gmuth.io.hexdump
import de.gmuth.ipp.core.IppTag.*
import de.gmuth.log.Logging
import de.gmuth.log.Logging.LogLevel.*
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.EOFException
import java.net.URI
import java.nio.charset.Charset

class IppInputStream(inputStream: BufferedInputStream) : DataInputStream(inputStream) {

    companion object {
        val log = Logging.getLogger {}
    }

    // character encoding for text and name attributes, RFC 8011 4.1.4.1
    internal lateinit var attributesCharset: Charset

    fun readMessage(message: IppMessage) {
        with(message) {
            version = "${read()}.${read()}"
            log.debug { "version = $version" }

            code = readShort()
            log.debug { "code = $code ($codeDescription)" }

            requestId = readInt()
            log.debug { "requestId = $requestId" }
        }

        lateinit var currentGroup: IppAttributesGroup
        lateinit var currentAttribute: IppAttribute<*>
        try {
            do {
                val tag = readTag()
                when {
                    tag.isGroupTag() -> {
                        currentGroup = message.createAttributesGroup(tag)
                    }
                    tag.isValueTag() -> {
                        val attribute = readAttribute(tag)
                        log.debug { "$attribute" }
                        if (attribute.name.isNotEmpty()) {
                            currentGroup.put(attribute, onReplaceWarn = true)
                            currentAttribute = attribute
                        } else { // name.isEmpty() -> 1setOf
                            currentAttribute.additionalValue(attribute)
                        }
                    }
                }
            } while (tag != End)
        } catch (exception: Exception) {
            readBytes().apply {
                log.warn { "skipped $size unparsed bytes" }
                hexdump { log.warn { it } }
            }
            throw exception
        }
    }

    internal fun readTag() =
        IppTag.fromByte(readByte()).apply {
            if (isDelimiterTag()) log.debug { "--- $this ---" }
        }

    internal fun readAttribute(tag: IppTag): IppAttribute<Any> {
        val name = readString()
        val value = try {
            readAttributeValue(tag)
        } catch (exception: Exception) {
            if (exception !is EOFException) readBytes().hexdump { log.info { it } }
            throw IppException("failed to read attribute value of '$name' ($tag)", exception)
        }
        // remember attributes-charset for name and text value decoding
        if (name == "attributes-charset") attributesCharset = value as Charset
        return IppAttribute(name, tag, value)
    }

    internal fun readAttributeValue(tag: IppTag): Any =
        when (tag) {

            IppTag.Boolean -> {
                readExpectedValueLength(1)
                readBoolean()
            }

            Integer,
            IppTag.Enum -> {
                readExpectedValueLength(4)
                readInt()
            }

            RangeOfInteger -> {
                readExpectedValueLength(8)
                IntRange(
                    start = readInt(),
                    endInclusive = readInt()
                )
            }

            Resolution -> {
                readExpectedValueLength(9)
                IppResolution(
                    x = readInt(),
                    y = readInt(),
                    unit = readByte().toInt()
                )
            }

            Charset -> Charset.forName(readString())

            Uri -> URI.create(readString())

            // String with rfc 8011 3.9 and rfc 8011 4.1.4.1 attribute value encoding
            Keyword,
            UriScheme,
            OctetString,
            MimeMediaType,
            MemberAttrName,
            NaturalLanguage -> readString()

            TextWithoutLanguage,
            NameWithoutLanguage -> IppString(readString(attributesCharset))

            TextWithLanguage,
            NameWithLanguage -> {
                readShort()
                IppString(
                    language = readString(attributesCharset),
                    text = readString(attributesCharset)
                )
            }

            DateTime -> {
                readExpectedValueLength(11)
                IppDateTime(
                    year = readShort().toInt(),
                    month = read(),
                    day = read(),
                    hour = read(),
                    minutes = read(),
                    seconds = read(),
                    deciSeconds = read(),
                    directionFromUTC = readByte().toInt().toChar(),
                    hoursFromUTC = read(),
                    minutesFromUTC = read()
                )
            }

            BegCollection -> {
                if (readExpectedValueLength(0, throwException = false)) {
                    readCollection()
                } else {
                    // Xerox B210: workaround for invalid 'media-col' without members
                    log.warn { "invalid value length for IppCollection, trying to recover" }
                    IppCollection()
                }
            }

            // for all other tags (including out-of-bound), read raw bytes (if present at all)
            else -> { // ByteArray - possibly empty
                readLengthAndValue().apply {
                    if (size > 0) {
                        val level = if (tag == Unsupported_) DEBUG else WARN
                        log.log(level) { "ignore $size value bytes tagged '$tag'" }
                        hexdump { log.log(level) { it } }
                    }
                }
            }
        }

    internal fun readCollection() = IppCollection().apply {
        lateinit var currentMemberAttribute: IppAttribute<Any>
        do {
            val attribute = readAttribute(readTag())
            when {
                attribute.tag.isMemberAttrName() -> {
                    val memberName = attribute.value as String
                    val firstValue = readAttribute(readTag())
                    currentMemberAttribute = IppAttribute(memberName, firstValue.tag, firstValue.value)
                    add(currentMemberAttribute)
                }
                attribute.tag.isMemberAttrValue() -> {
                    currentMemberAttribute.additionalValue(attribute)
                }
            }
        } while (attribute.tag != EndCollection)
    }

    // RFC 8011 4.1.4.1 -> use attributes-charset
    internal fun readString(charset: Charset = Charsets.US_ASCII) =
        String(readLengthAndValue(), charset)

    internal fun readLengthAndValue() =
        readBytes(readShort().toInt())

    // avoid Java-11-readNBytes(length) for compatibility with older jvms
    internal fun readBytes(length: Int) =
        ByteArray(length).apply {
            log.trace { "read $length bytes" }
            readFully(this)
        }

    internal fun readExpectedValueLength(expected: Int, throwException: Boolean = true): Boolean {
        mark(2)
        val length = readShort().toInt()
        return (length == expected).apply {
            if (!this) { // unexpected value length
                reset() // revert 'readShort()'
                with("expected value length of $expected bytes but found $length") {
                    if (throwException) throw IppException(this) else log.warn { this }
                }
            }
        }
    }

}