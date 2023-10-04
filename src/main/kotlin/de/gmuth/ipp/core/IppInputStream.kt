package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppTag.*
import java.io.*
import java.net.URI
import java.nio.charset.Charset
import java.util.logging.Level
import java.util.logging.Logger.getLogger

class IppInputStream(inputStream: BufferedInputStream) : DataInputStream(inputStream) {

    private val log = getLogger(javaClass.name)

    // character encoding for text and name attributes, RFC 8011 4.1.4.1
    internal lateinit var attributesCharset: Charset

    fun readMessage(message: IppMessage) {
        with(message) {
            version = "${readUnsignedByte()}.${readUnsignedByte()}"
            log.finest { "version = $version" }

            code = readUnsignedShort()
            log.finest { "code = $code ($codeDescription)" }

            requestId = readInt()
            log.finest { "requestId = $requestId" }
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
                        log.finest { attribute.toString() }
                        if (attribute.name.isNotEmpty()) {
                            currentGroup.put(attribute)
                            currentAttribute = attribute
                        } else { // name.isEmpty() -> 1setOf
                            currentAttribute.additionalValue(attribute)
                        }
                    }
                }
            } while (tag != End)
        } catch (throwable: Throwable) {
            if (throwable !is EOFException) readBytes().apply {
                if (isNotEmpty()) {
                    log.warning { "Skipped $size unparsed bytes" }
                    hexdump { log.finest { it } }
                }
            }
            throw IppException("Failed to read ipp message", throwable)
        }
    }

    internal fun readTag() = IppTag.fromByte(readByte()).apply {
        if (isDelimiterTag()) log.finest { "--- $this ---" }
    }

    internal fun readAttribute(tag: IppTag) = IppAttribute<Any>(name = readString(), tag).apply {
        try {
            values.add(readAttributeValue(tag))
        } catch (throwable: Throwable) {
            throw IppException("Failed to read attribute value for '$name' ($tag)", throwable)
        }
        // remember attributes-charset for name and text value decoding
        if (name == "attributes-charset") attributesCharset = value as Charset
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

            Charset -> {
                Charset.forName(readString())
            }

            Uri -> {
                URI.create(readString().replace(" ", "%20"))
            }

            // String with rfc 8011 3.9 and rfc 8011 4.1.4.1 attribute value encoding
            Keyword,
            UriScheme,
            OctetString,
            MimeMediaType,
            MemberAttrName,
            NaturalLanguage -> {
                readString()
            }

            TextWithoutLanguage,
            NameWithoutLanguage -> {
                IppString(readString(attributesCharset))
            }

            TextWithLanguage,
            NameWithLanguage -> {
                mark(2)
                readShort().let { if (it < 6) reset() } // HP M175nw: support invalid ipp response
                IppString(
                    language = readString(attributesCharset),
                    text = readString(attributesCharset)
                )
            }

            DateTime -> {
                readExpectedValueLength(11)
                IppDateTime(
                    year = readShort().toInt(),
                    month = readUnsignedByte(),
                    day = readUnsignedByte(),
                    hour = readUnsignedByte(),
                    minutes = readUnsignedByte(),
                    seconds = readUnsignedByte(),
                    deciSeconds = readUnsignedByte(),
                    directionFromUTC = readByte().toInt().toChar(),
                    hoursFromUTC = readUnsignedByte(),
                    minutesFromUTC = readUnsignedByte()
                )
            }

            BegCollection -> {
                if (readExpectedValueLength(0, throwException = false)) {
                    readCollection()
                } else {
                    // Xerox B210: workaround for invalid 'media-col' without members
                    log.warning { "Invalid value length for IppCollection, trying to recover" }
                    IppCollection()
                }
            }

            // for all other tags (including out-of-bound), read raw bytes (if present at all)
            else -> { // ByteArray - possibly empty
                readLengthAndValue().apply {
                    if (isNotEmpty()) {
                        val level = if (tag == Unsupported_) Level.FINEST else Level.WARNING
                        log.log(level) { "Ignore $size value bytes tagged '$tag'" }
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
        readBytes(readUnsignedShort())

    // avoid readNBytes(length) for compatibility with JREs < 11
    internal fun readBytes(length: Int) = ByteArray(length).apply {
        readFully(this)
    }

    internal fun readExpectedValueLength(expected: Int, throwException: Boolean = true): Boolean {
        mark(2)
        val length = readUnsignedShort()
        return (length == expected).apply {
            if (!this) { // unexpected value length
                reset() // revert 'readShort()'
                with("Expected value length of $expected bytes but found $length") {
                    if (throwException) throw IppException(this) else log.warning { this }
                }
            }
        }
    }

    fun ByteArray.hexdump(maxRows: Int = 32, dump: (String) -> Unit) {
        val hexStringBuilder = StringBuilder()
        val charStringBuilder = StringBuilder()
        fun dumpLine() = dump("%-${maxRows * 3}s  '%s'".format(hexStringBuilder, charStringBuilder))
        for ((index, b) in withIndex()) {
            hexStringBuilder.append("%02X ".format(b))
            charStringBuilder.append(b.toInt().toChar())
            if ((index + 1) % maxRows == 0) {
                dumpLine()
                hexStringBuilder.clear()
                charStringBuilder.clear()
            }
        }
        if (isNotEmpty()) dumpLine()
    }

}