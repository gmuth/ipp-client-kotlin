package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

// [RFC 8010] and [RFC 3380]
enum class IppTag(val code: Byte, private val ianaName: String? = null) {

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

    // Int
    Integer(0x21),
    Boolean(0x22),
    Enum(0x23),

    // misc
    OctetString(0x30),
    DateTime(0x31),
    Resolution(0x32),
    RangeOfInteger(0x33),
    BegCollection(0x34),
    TextWithLanguage(0x35),
    NameWithLanguage(0x36),
    EndCollection(0x37),

    // String
    TextWithoutLanguage(0x41),
    NameWithoutLanguage(0x42),
    Keyword(0x44),
    Uri(0x45),
    UriScheme(0x46),
    Charset(0x47),
    NaturalLanguage(0x48),
    MimeMediaType(0x49),
    MemberAttrName(0x4A);

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

}