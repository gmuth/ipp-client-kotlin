package de.gmuth.ipp.iana

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.csv.CSVTable
import de.gmuth.ipp.core.IppTag
import de.gmuth.log.Logging

/**
 * https://www.iana.org/assignments/ipp-registrations/ipp-registrations.xml#ipp-registrations-2
 */
object IppRegistrationsSection2 {

    data class Attribute(
            val collection: String,
            val name: String,
            val memberAttribute: String,
            val subMemberAttribute: String,
            val syntax: String,
            val reference: String
    ) {
        constructor(columns: List<String>) : this(
                collection = columns[0],
                name = columns[1],
                memberAttribute = columns[2],
                subMemberAttribute = columns[3],
                syntax = columns[4],
                reference = columns[5]
        )

        override fun toString() = String.format("%-30s%-70s%s", collection, key(), syntax)

        fun is1setOf() = syntax.contains("1setOf")

        fun tag() = when {
            syntax.contains("naturalLanguage") -> IppTag.NaturalLanguage
            syntax.contains("mimeMediaType") -> IppTag.MimeMediaType
            syntax.contains("charset") -> IppTag.Charset
            syntax.contains("uri") -> IppTag.Uri
            syntax.contains("uriScheme") -> IppTag.UriScheme
            syntax.contains("octetString") -> IppTag.OctetString
            syntax.contains("keyword") -> IppTag.Keyword
            syntax.contains("name") -> IppTag.NameWithoutLanguage
            syntax.contains("text") -> IppTag.TextWithoutLanguage
            syntax.contains("memberAttrName") -> IppTag.MemberAttrName
            syntax.contains("integer") -> IppTag.Integer
            syntax.contains("enum") -> IppTag.Enum
            syntax.contains("boolean") -> IppTag.Boolean
            syntax.contains("rangeOfInteger") -> IppTag.RangeOfInteger
            syntax.contains("dateTime") -> IppTag.DateTime
            syntax.contains("resolution") -> IppTag.Resolution
            syntax.contains("collection") -> IppTag.BegCollection
            syntax.isEmpty() -> null
            else -> throw IllegalStateException("'$name' has unknown syntax '$syntax'")
        }

        // key for map (name is not unique)
        fun key() = StringBuffer(name).apply {
            if (memberAttribute.isNotBlank()) append("/$memberAttribute")
            if (subMemberAttribute.isNotBlank()) append("/$subMemberAttribute")
        }.toString()

        fun collectionGroupTag() = when (collection) {
            "Operation" -> IppTag.Operation
            "Job Template" -> IppTag.Job
            else -> throw IllegalArgumentException("no IppTag defined for $collection")
        }

    }

    val log = Logging.getLogger {}

    // source: https://www.iana.org/assignments/ipp-registrations/ipp-registrations-2.csv
    val allAttributes = CSVTable("/ipp-registrations-2.csv", ::Attribute).rows

    val attributesMap = allAttributes.associateBy(Attribute::key)

    fun tagForAttribute(name: String) = attributesMap[name]?.tag()

    fun attributeIs1setOf(name: String) = attributesMap[name]?.is1setOf()

    fun checkSyntaxOfAttribute(name: String, tag: IppTag) {
        if (tag.isOutOfBandTag()) return
        val syntax = attributesMap[name]?.syntax
        if (syntax != null && syntax.isNotEmpty() && !syntax.contains(tag.registeredSyntax())) {
            log.warn { "$name ($tag) does not match syntax '$syntax'" }
        }
    }

    fun selectGroupForAttribute(name: String) = attributesMap[name]?.collectionGroupTag() ?: IppTag.Job

}