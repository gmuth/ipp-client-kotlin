package de.gmuth.ipp

/**
 * Author: Gerhard Muth
 */

enum class IppTag(val value: Byte) {

    // Group Tags
    Operation(0x01),
    Job(0x02),
    End(0x03),
    Printer(0x04),
    Unsupported(0x05),
    Subscription(0x06),
    EventNotification(0x07),

    // Attribute Tags
    Unsupported_(0x10),
    Unknown(0x12),
    NoValue(0x13),
    DeleteAttribute(0x16),
    Integer(0x21),
    Boolean(0x22),
    Enum(0x23),
    Octet(0x30),
    DateTime(0x31),
    Resolution(0x32),
    RangeOfInteger(0x33),
    BeginCollection(0x34),
    TextWithLanguage(0x35),
    NameWithLanguage(0x36),
    EndCollection(0x37),
    GenericCharactersString(0x40),
    TextWithoutLanguage(0x41),
    NameWithoutLanguage(0x42),
    Keyword(0x44),
    Uri(0x45),
    UriScheme(0x46),
    Charset(0x47),
    NaturalLanguage(0x48),
    MimeMediaType(0x49),
    MemberAttrName(0x4A);

    fun isGroupTag(): kotlin.Boolean = value < 0x10

    override fun toString(): String = name.toLowerCase()

    companion object {
        private val map = values().associateBy(IppTag::value)
        fun fromByte(value: Byte): IppTag = map[value] ?: throw IllegalArgumentException(String.format("ipp tag %02X undefined", value))
    }

}