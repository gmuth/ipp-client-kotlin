package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppTag.*
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.EOFException
import java.net.URI
import java.nio.charset.Charset
import java.util.logging.Logger.getLogger

class IppInputStream(inputStream: BufferedInputStream) : DataInputStream(inputStream) {

    companion object {
        var readAttribute_ignoreException: Boolean = false
    }

    internal val logger = getLogger(javaClass.name)

    // character encoding for text and name attributes, RFC 8011 4.1.4.1
    internal lateinit var attributesCharset: Charset

    fun readMessage(message: IppMessage) {
        with(message) {
            version = "${readUnsignedByte()}.${readUnsignedByte()}"
            logger.finer { "version = $version" }

            code = readUnsignedShort()
            logger.finer { "code = $code ($codeDescription)" }

            requestId = readInt()
            logger.finer { "requestId = $requestId" }
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
                        logger.finer { attribute.toString() }
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
                    logger.warning { "Skipped $size unparsed bytes" }
                    hexdump { logger.finer { it } }
                }
            }
            throw IppException("Failed to read ipp message", throwable)
        }
    }

    internal fun readTag() = IppTag.fromByte(readByte()).apply {
        if (isDelimiterTag()) logger.finer { "--- $this ---" }
    }

    internal fun readAttribute(tag: IppTag) = IppAttribute<Any>(name = readString(), tag).apply {
        try {
            values.add(readAttributeValue(tag))
        } catch (throwable: Throwable) {
            val message = "Failed to read attribute value for '$name' ($tag)"
            if (readAttribute_ignoreException) logger.warning { "Ignore exception: $message: ${throwable.toString()}" }
            else throw IppException(message, throwable)
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
                val uriString = readString().replace(" ", "%20")
                try {
                    URI.create(uriString)
                } catch (throwable: Throwable) {
                    logger.fine { "readAttributeValue($tag): $throwable" }
                    uriString // workaround: return String instead of URI
                }
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
                    logger.warning { "Invalid value length for IppCollection, trying to recover" }
                    IppCollection()
                }
            }

            // for all other tags (including out-of-bound), read raw bytes (if present at all)
            else -> { // ByteArray - possibly empty
                readLengthAndValue().apply {
                    if (isNotEmpty()) {
                        logger.finest { "Ignore $size value bytes tagged '$tag'" }
                        hexdump { logger.finest { it } }
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
            .also { logger.finest { "readString($charset): \"$it\"" } }

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
                    if (throwException) throw IppException(this) else logger.warning { this }
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
        if (hexStringBuilder.isNotEmpty()) dumpLine()
    }

}