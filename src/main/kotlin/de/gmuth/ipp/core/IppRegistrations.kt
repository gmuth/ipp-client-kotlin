package de.gmuth.ipp.core

class IppRegistrations {
    companion object {

        fun ippTag(tagName: String): IppTag = when (tagName) {
            "operation-attributes-tag" -> IppTag.Operation
            "charset" -> IppTag.Charset
            "language" -> IppTag.NaturalLanguage
            "uri" -> IppTag.Uri

            else -> throw IllegalArgumentException("unknown tag '$tagName'")
        }

        fun ippTagForAttribute(attributeName: String): IppTag = when (attributeName) {
            "attributes-charset" -> IppTag.Charset
            "attributes-natural-language" -> IppTag.NaturalLanguage
            "printer-uri" -> IppTag.Uri
            "document-format" -> IppTag.MimeMediaType
            "requesting-user-name" -> IppTag.NameWithoutLanguage

            else -> throw IllegalArgumentException("tag for attribute '$attributeName' not found, sorry - you have to specify the tag!")
        }
    }
}