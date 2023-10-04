package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

// [RFC 8010] and [RFC 3380]
enum class IppTag(
    val code: Byte,
    val registeredName: String,
    val valueHasValidClass: (Any) -> kotlin.Boolean = { true }
) {
    // Delimiter tags
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

    // Out-of-band tags
    // https://www.iana.org/assignments/ipp-registrations/ipp-registrations.xml#ipp-registrations-8
    Unsupported_(0x10, "unsupported"),
    Unknown(0x12, "unknown"),
    NoValue(0x13, "no-value"),
    NotSettable(0x15, "not-settable"),
    DeleteAttribute(0x16, "delete-attribute"),
    AdminDefine(0x17, "admin-define"),

    //https://www.iana.org/assignments/ipp-registrations/ipp-registrations.xml#ipp-registrations-9

    // Integer
    Integer(0x21, "integer", { it is Number }),
    Boolean(0x22, "boolean", { it is kotlin.Boolean }),
    Enum(0x23, "enum", { it is Int }),

    // Misc
    OctetString(0x30, "octetString", { it is String }),
    DateTime(0x31, "dateTime", { it is IppDateTime }),
    Resolution(0x32, "resolution", { it is IppResolution }),
    RangeOfInteger(0x33, "rangeOfInteger", { it is IntRange }),
    BegCollection(0x34, "collection", { it is IppCollection }),
    TextWithLanguage(0x35, "textWithLanguage", { it is IppString }),
    NameWithLanguage(0x36, "nameWithLanguage", { it is IppString }),
    EndCollection(0x37, "endCollection"),

    // Text
    TextWithoutLanguage(0x41, "textWithoutLanguage", { it is IppString || it is String }),
    NameWithoutLanguage(0x42, "nameWithoutLanguage", { it is IppString || it is String }),
    Keyword(0x44, "keyword", { it is String }),
    Uri(0x45, "uri", { it is java.net.URI }),
    UriScheme(0x46, "uriScheme", { it is String }),
    Charset(0x47, "charset", { it is java.nio.charset.Charset }),
    NaturalLanguage(0x48, "naturalLanguage", { it is String }),
    MimeMediaType(0x49, "mimeMediaType", { it is String }),
    MemberAttrName(0x4A, "memberAttrName", { it is String });

    fun isDelimiterTag() = code < 0x10
    fun isGroupTag() = code < 0x10 && this != End
    fun isValueTag() = 0x10 <= code
    fun isOutOfBandTag() = code in 0x10..0x1f
    fun isMemberAttrName() = this == MemberAttrName
    fun isMemberAttrValue() = this != MemberAttrName && isValueTag() && this != EndCollection

    override fun toString() = registeredName

    fun registeredSyntax() = when (this) {
        // iana registered syntax doesn't care about language
        NameWithoutLanguage, NameWithLanguage -> "name"
        TextWithoutLanguage, TextWithLanguage -> "text"
        else -> registeredName
    }

    fun validateValueClass(value: Any) {
        if (!valueHasValidClass(value)) throw IppException("Value class ${value::class.java.name} not valid for tag $this")
    }

    companion object {
        fun fromByte(code: Byte): IppTag =
            values().singleOrNull { it.code == code } ?: throw IppException("Unknown tag 0x%02X".format(code))

        fun fromString(name: String): IppTag =
            values().singleOrNull { it.registeredName == name } ?: throw IppException("Unknown tag name '$name'")
    }

}