package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

// [RFC 8010] and [RFC 3380]
enum class IppTag(
        val code: Byte,
        private val registeredName: String
) {
    // delimiter tags
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
    Integer(0x21, "integer"),
    Boolean(0x22, "boolean"),
    Enum(0x23, "enum"),

    // Misc
    OctetString(0x30, "octetString"),
    DateTime(0x31, "dateTime"),
    Resolution(0x32, "resolution"),
    RangeOfInteger(0x33, "rangeOfInteger"),
    BegCollection(0x34, "collection"),
    TextWithLanguage(0x35, "textWithLanguage"),
    NameWithLanguage(0x36, "nameWithLanguage"),
    EndCollection(0x37, "endCollection"),

    // Text
    TextWithoutLanguage(0x41, "textWithoutLanguage"),
    NameWithoutLanguage(0x42, "nameWithoutLanguage"),
    Keyword(0x44, "keyword"),
    UriScheme(0x46, "uriScheme"),
    Uri(0x45, "uri"),
    Charset(0x47, "charset"),
    NaturalLanguage(0x48, "naturalLanguage"),
    MimeMediaType(0x49, "mimeMediaType"),
    MemberAttrName(0x4A, "memberAttrName");

    fun isDelimiterTag() = code in 0x00..0x0F
    fun isOutOfBandTag() = code in 0x10..0x1F
    fun isValueTag() = code in 0x20..0x4F
    fun isEndTag() = this == End

    fun selectCharset(attributesCharset: java.nio.charset.Charset?) =
            if (this in listOf(TextWithoutLanguage, TextWithLanguage, NameWithoutLanguage, NameWithLanguage)) {
                attributesCharset ?: throw IppException("missing attributes-charset")
            } else {
                Charsets.US_ASCII
            }

    fun registeredSyntax() = when (this) {
        // iana registered syntax doesn't care about language
        NameWithoutLanguage, NameWithLanguage -> "name"
        TextWithoutLanguage, TextWithLanguage -> "text"
        else -> registeredName
    }

    override fun toString() = registeredName

    companion object {
        private val codeMap = values().associateBy(IppTag::code)
        fun fromByte(code: Byte): IppTag = codeMap[code]
                ?: throw IppException(String.format("ipp tag code '%02X' unknown", code))

        private val registeredNameMap = values().associateBy(IppTag::registeredName)
        fun fromRegisteredName(name: String): IppTag = registeredNameMap[name]
                ?: throw IppException(String.format("ipp tag name '%s' unknown", name))
    }

}