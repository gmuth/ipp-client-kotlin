package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class IppRegistrations {
    companion object {
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