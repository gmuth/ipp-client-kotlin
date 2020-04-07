package de.gmuth.ipp.core

import java.net.URI
import kotlin.reflect.KClass

/**
 * Copyright (c) 2020 Gerhard Muth
 */

// [RFC 8010] and [RFC 3380]
enum class IppTag(
        val code: Byte,
        private val ianaName: String? = null,
        val valueClass: KClass<*>? = null

) {
    // attribute group tags
    // https://www.iana.org/assignments/ipp-registrations/ipp-registrations.xml#ipp-registrations-7
    Operation(0x01, "operation-attributes-tag"),
    Job(0x02, "job-attributes-tag"),
    End(0x03, "end-of-attributes-tag"),
    Printer(0x04, "printer-attributes-tag"),
    Unsupported(0x05, "unsupported-attributes-tag"),
    Subscription(0x06, "subscription-attributes-tag"),
    EventNotification(0x07, "event-notification-attributes-tag"),
    Resource(0x08, "resource-attributes-tag"),
    Document(0x09, "document-attributes-tag"),
    System(0x0A, "system-attributes-tag"),

    // out-of-band tags
    // https://www.iana.org/assignments/ipp-registrations/ipp-registrations.xml#ipp-registrations-8
    Unsupported_(0x10, "unsupported"),
    Unknown(0x12, "unknown"),
    NoValue(0x13, "no-value"),
    NotSettable(0x15, "not-settable"),
    DeleteAttribute(0x16, "delete-attribute"),
    AdminDefine(0x17, "admin-define"),

    //https://www.iana.org/assignments/ipp-registrations/ipp-registrations.xml#ipp-registrations-9

    // Integer
    Integer(0x21, valueClass = Int::class),
    Boolean(0x22),//kotlin.Boolean::class),
    Enum(0x23, valueClass = Int::class), // Enum?

    // Misc
    OctetString(0x30),
    DateTime(0x31),
    Resolution(0x32),
    RangeOfInteger(0x33),
    BegCollection(0x34),
    TextWithLanguage(0x35),
    NameWithLanguage(0x36),
    EndCollection(0x37),

    // Text
    TextWithoutLanguage(0x41, valueClass = String::class),
    NameWithoutLanguage(0x42, valueClass = String::class),
    Keyword(0x44, valueClass = String::class),
    UriScheme(0x46, valueClass = String::class),
    Uri(0x45, valueClass = URI::class),
    Charset(0x47, valueClass = String::class), // java.nio.Charset issue: sun.nio.cs.UTF_8
    NaturalLanguage(0x48, valueClass = String::class),
    MimeMediaType(0x49, valueClass = String::class),
    MemberAttrName(0x4A, valueClass = String::class);

    fun isGroupTag() = code in 0x00..0x0F
    fun isOutOfBandTag() = code in 0x10..0x1F
    fun isIntegerTag() = code in 0x20..0x2F
    fun isStringTag() = code in 0x40..0x4F
    fun isCollection() = this in listOf(BegCollection, EndCollection, MemberAttrName)

    fun useAttributesCharset() = this in listOf(TextWithoutLanguage, TextWithLanguage, NameWithoutLanguage, NameWithLanguage)

    private fun registeredName() = ianaName
            ?: name.replace("^[A-Z]".toRegex()) { it.value.toLowerCase() }

    override fun toString() = registeredName()

    companion object {
        private val codeMap = values().associateBy(IppTag::code)
        fun fromCode(code: Byte): IppTag = codeMap[code]
                ?: throw IppException(String.format("ipp tag code '%02X' unknown", code))

        private val registeredNameMap = values().associateBy(IppTag::registeredName)
        fun fromRegisteredName(name: String): IppTag = registeredNameMap[name]
                ?: throw IppException(String.format("ipp tag name '%s' unknown", name))
    }

    fun registeredSyntax() = when (this) {
        // iana registration doesn't care about language
        NameWithoutLanguage, NameWithLanguage -> "name"
        TextWithoutLanguage, TextWithLanguage -> "text"
        else -> registeredName()
    }

    // reflection issue: Kotlin reflection is not available
    // class java.nio.charset.Charset != class sun.nio.cs.UTF_8
    fun no_validateValueClass(value: Any?) {
        if (value != null && valueClass != null && (value::class) == valueClass) {
            throw IppException("expected value of $valueClass but found: ${value::class}")
        }
    }

}