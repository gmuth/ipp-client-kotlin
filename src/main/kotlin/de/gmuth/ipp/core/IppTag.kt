package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

enum class IppTag(val code: Byte, private val ianaName: String? = null) {

    // attribute group tags
    // https://www.iana.org/assignments/ipp-registrations/ipp-registrations.xml#ipp-registrations-7
    Operation(0x01, "operation-attributes-tag"),
    Job(0x02, "job-attributes-tag"),
    End(0x03, "end-of-attributes-tag"),
    Printer(0x04, "printer-attributes-tag"),
    Unsupported_(0x05, "unsupported-attributes-tag"),
    Subscription(0x06, "subscription-attributes-tag"),
    EventNotification(0x07, "event-notification-attributes-tag"),
    Resource(0x08, "resource-attributes-tag"),
    Document(0x09, "document-attributes-tag"),
    System(0x0A, "system-attributes-tag"),

    // out-of-band tags
    // https://www.iana.org/assignments/ipp-registrations/ipp-registrations.xml#ipp-registrations-8
    Unsupported(0x10),
    Unassigned(0x11),
    Unknown(0x12),
    NoValue(0x13),
    NotSettable(0x15),
    DeleteAttribute(0x16),
    AdminDefine(0x17),

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

    fun isGroupTag() = code < 0x10

    private fun registeredName() = ianaName
            ?: name.replace("^[A-Z]".toRegex()) { it.value.toLowerCase() }

    override fun toString() = registeredName()

    companion object {
        private val codeMap = values().associateBy(IppTag::code)
        fun fromCode(code: Byte): IppTag = codeMap[code]
                ?: throw IllegalArgumentException(String.format("ipp tag code '%02X' unknown", code))

        private val registeredNameMap = values().associateBy(IppTag::registeredName)
        fun fromRegisteredName(name: String): IppTag = registeredNameMap[name]
                ?: throw IllegalArgumentException(String.format("ipp tag name '%s' unknown", name))

    }

}