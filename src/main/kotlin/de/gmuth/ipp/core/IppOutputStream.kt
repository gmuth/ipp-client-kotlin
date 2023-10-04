package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppTag.*
import java.io.DataOutputStream
import java.io.OutputStream
import java.net.URI
import java.nio.charset.Charset
import java.util.logging.Logger.getLogger

class IppOutputStream(outputStream: OutputStream) : DataOutputStream(outputStream) {

    private val log = getLogger(javaClass.name)

    // Charset for text and name attributes, RFC 8011 4.1.4.1
    internal lateinit var attributesCharset: Charset

    fun writeMessage(message: IppMessage) = message.run {
        attributesCharset = operationGroup.getValue("attributes-charset")

        writeVersion(version ?: throw IppException("missing version"))
        log.finest { "version = $version" }

        writeShort(code ?: throw IppException("missing operation or status code"))
        log.finest { "code = $code ($codeDescription)" }

        writeInt(requestId ?: throw IppException("missing requestId"))
        log.finest { "requestId = $requestId" }

        for (group in attributesGroups) {
            writeTag(group.tag)
            for (attribute in group.values) {
                try {
                    writeAttribute(attribute)
                } catch (exception: Exception) {
                    throw IppException("failed to write attribute: $attribute", exception)
                }
            }
        }
        writeTag(End)
    }

    internal fun writeVersion(version: String) {
        with(Regex("""^(\d)\.(\d)$""").find(version)!!) {
            writeByte(groups[1]!!.value.toInt())
            writeByte(groups[2]!!.value.toInt())
        }
    }

    internal fun writeTag(tag: IppTag) {
        if (tag.isDelimiterTag()) log.finest { "--- $tag ---" }
        writeByte(tag.code.toInt())
    }

    internal fun writeString(string: String, charset: Charset = Charsets.US_ASCII) {
        with(string.toByteArray(charset)) {
            writeShort(size)
            write(this)
        }
    }

    internal fun writeAttribute(attribute: IppAttribute<*>) {
        log.finest { "$attribute" }
        with(attribute) {
            if (values.isEmpty() || tag.isOutOfBandTag()) {
                writeTag(tag)
                writeString(name)
                writeShort(0) // no value
            } else {
                // single value or 1setOf values
                for ((index, value) in values.withIndex()) {
                    writeTag(tag)
                    writeString(if (index == 0) name else "")
                    writeAttributeValue(tag, value!!)
                }
            }
        }
    }

    internal fun writeAttributeValue(tag: IppTag, value: Any) {
        when (tag) {

            IppTag.Boolean -> with(value as Boolean) {
                writeShort(1)
                writeBoolean(value)
            }

            Integer,
            IppTag.Enum -> with(value as Number) { // Int or Short expected
                writeShort(4)
                writeInt(value.toInt())
            }

            RangeOfInteger -> with(value as IntRange) {
                writeShort(8)
                writeInt(start)
                writeInt(endInclusive)
            }

            Resolution -> with(value as IppResolution) {
                writeShort(9)
                writeInt(x)
                writeInt(y)
                writeByte(unit)
            }

            Charset -> with(value as Charset) {
                writeString(name().lowercase())
            }

            Uri -> with(value as URI) {
                writeString(value.toString())
            }

            Keyword,
            UriScheme,
            OctetString,
            MimeMediaType,
            MemberAttrName,
            NaturalLanguage -> with(value as String) {
                writeString(value)
            }

            TextWithoutLanguage,
            NameWithoutLanguage -> when (value) {
                is String -> writeString(value, attributesCharset)
                is IppString -> writeString(value.text, attributesCharset)
                else -> throw IppException("expecting value class String or IppString")
            }

            TextWithLanguage,
            NameWithLanguage -> with(value as IppString) {
                if (language == null) throw IppException("expecting IppString with language")
                writeShort(4 + text.length + language.length)
                writeString(language, attributesCharset)
                writeString(text, attributesCharset)
            }

            DateTime -> with(value as IppDateTime) {
                writeShort(11)
                writeShort(year)
                writeByte(month)
                writeByte(day)
                writeByte(hour)
                writeByte(minutes)
                writeByte(seconds)
                writeByte(deciSeconds)
                writeByte(directionFromUTC.code)
                writeByte(hoursFromUTC)
                writeByte(minutesFromUTC)
            }

            BegCollection -> with(value as IppCollection) {
                writeShort(0)
                for (member in members) {
                    writeAttribute(IppAttribute("", MemberAttrName, member.name))
                    for (memberValue in member.values) {
                        writeAttribute(IppAttribute("", member.tag, memberValue))
                    }
                }
                writeAttribute(IppAttribute<Unit>("", EndCollection))
            }

            else -> throw IppException("Unknown tag 0x%02X %s".format(tag.code, tag))
        }
    }
}