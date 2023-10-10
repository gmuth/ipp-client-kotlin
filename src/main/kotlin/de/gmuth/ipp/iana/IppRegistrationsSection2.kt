package de.gmuth.ipp.iana

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttribute
import de.gmuth.ipp.core.IppCollection
import de.gmuth.ipp.core.IppMessage
import de.gmuth.ipp.core.IppTag
import de.gmuth.ipp.core.IppTag.*
import java.util.logging.Logger.getLogger

// https://www.iana.org/assignments/ipp-registrations/ipp-registrations.xml#ipp-registrations-2
object IppRegistrationsSection2 {

    val log = getLogger(javaClass.name)

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
            syntax.contains("naturalLanguage") -> NaturalLanguage
            syntax.contains("mimeMediaType") -> MimeMediaType
            syntax.contains("charset") -> Charset
            syntax.contains("uri") -> Uri
            syntax.contains("uriScheme") -> UriScheme
            syntax.contains("octetString") -> OctetString
            syntax.contains("keyword") -> Keyword
            syntax.contains("name") -> NameWithoutLanguage
            syntax.contains("text") -> TextWithoutLanguage
            syntax.contains("memberAttrName") -> MemberAttrName
            syntax.contains("integer") -> Integer
            syntax.contains("enum") -> IppTag.Enum
            syntax.contains("boolean") -> IppTag.Boolean
            syntax.contains("rangeOfInteger") -> RangeOfInteger
            syntax.contains("dateTime") -> DateTime
            syntax.contains("resolution") -> Resolution
            syntax.contains("collection") -> BegCollection
            syntax.isEmpty() -> null
            else -> error("'$name' has unknown syntax '$syntax'")
        }

        // key for map (name is not unique)
        fun key() = StringBuffer(name).apply {
            if (memberAttribute.isNotBlank()) append("/$memberAttribute")
            if (subMemberAttribute.isNotBlank()) append("/$subMemberAttribute")
        }.toString()

        fun collectionGroupTag() = when (collection) {
            "Operation" -> Operation
            "Job Template" -> Job
            else -> error("No IppTag defined for $collection")
        }

    }

    private val attributesMap: Map<String, Attribute>
    private val aliasMap: Map<String, String>

    init {
        // source: https://www.iana.org/assignments/ipp-registrations/ipp-registrations-2.csv
        val allAttributes = CSVTable("/ipp-registrations-2.csv", ::Attribute).rows

        attributesMap = allAttributes.associateBy(Attribute::key)

        //  alias example: Printer Description,media-col-default,"<Member attributes are the same as the ""media-col"" Job Template attribute>"
        aliasMap = mutableMapOf<String, String>().apply {
            allAttributes
                .filter { it.memberAttribute.lowercase().contains("same as") }
                .forEach { put(it.name, it.memberAttribute.replace("^.*\"(.+)\".*$".toRegex(), "$1")) }
            // apple cups extension 'output-mode' was standardized to 'print-color-mode'
            put("output-mode-default", "print-color-mode-default")
            put("output-mode-supported", "print-color-mode-supported")
            // 'media-col-default' resolves to 'media-col' and 'media-source-feed-...' values are registered for 'media-col-ready'
            put(
                "media-col/media-source-properties/media-source-feed-direction",
                "media-col-ready/media-source-properties/media-source-feed-direction"
            )
            put(
                "media-col/media-source-properties/media-source-feed-orientation",
                "media-col-ready/media-source-properties/media-source-feed-orientation"
            )
        }
    }

    fun resolveAlias(name: String) = (aliasMap[name] ?: name).also {
        if (aliasMap.containsKey(name)) log.finer { "'$name' resolves to '$it'" }
    }

    fun getAttribute(name: String, resolveAlias: Boolean = true) =
        attributesMap[if (resolveAlias) resolveAlias(name) else name]

    fun syntaxForAttribute(name: String, resolveAlias: Boolean) =
        getAttribute(name, resolveAlias)?.syntax

    fun tagForAttribute(name: String) = getAttribute(name)?.tag()

    fun attributeIs1setOf(name: String) =
        getAttribute(name, false)?.is1setOf()

    fun selectGroupForAttribute(name: String) =
        getAttribute(name, false)?.collectionGroupTag()

    val unknownAttributes = mutableSetOf<String>()

    fun checkSyntaxOfAttribute(name: String, tag: IppTag) {
        if (tag.isOutOfBandTag()) return
        val syntax = syntaxForAttribute(name, true)
        if (syntax == null) {
            log.fine { "no syntax found for '$name'" }
            unknownAttributes.add(name)
        } else if (!syntax.contains(tag.registeredSyntax())) {
            log.warning { "$name ($tag) does not match iana registered syntax '$syntax'" }
        }
    }

    fun validate(ippMessage: IppMessage) {
        for (group in ippMessage.attributesGroups) {
            for (attribute in group.values) {
                validate(attribute)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun validate(ippAttribute: IppAttribute<*>) = with(ippAttribute) {
        checkSyntaxOfAttribute(name, tag)
        if (isCollection()) validate(name, values as List<IppCollection>)
        if (!tag.isOutOfBandTag() && values.isEmpty()) log.warning { "'$name' ($tag) has no values" }
        if (values.size > 1 && attributeIs1setOf(name) == false) log.warning { "'$name' is not registered as '1setOf'" }
    }

    @Suppress("UNCHECKED_CAST")
    fun validate(name: String, ippCollections: List<IppCollection>) {
        log.finer { "validate collection '$name'" }
        val resolvedName = resolveAlias(name)
        for (ippCollection in ippCollections) {
            log.finer { "         ${ippCollection.members.size} members" }
            for (member in ippCollection.members) {
                if (member.isCollection()) {
                    validate("$resolvedName/${member.name}", member.values as List<IppCollection>)
                } else {
                    checkSyntaxOfAttribute("$resolvedName/${member.name}", member.tag)
                }
            }
        }
    }

    fun logUnknownAttributes() {
        with(unknownAttributes.toMutableList()) {
            sort()
            log.info { "$size unknown attributes:" }
            forEach { log.info { "- $it" } }
        }
    }
}