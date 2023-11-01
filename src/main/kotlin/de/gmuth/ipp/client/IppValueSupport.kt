package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.*
import de.gmuth.ipp.core.IppTag.*
import java.util.logging.Logger.getLogger

// ------------------------------------------------------
// Attribute value checking based on printer capabilities
// ------------------------------------------------------

object IppValueSupport {

    private val log = getLogger(javaClass.name)

    fun checkIfValueIsSupported(printerAttributes: IppAttributesGroup, attribute: IppAttribute<Any>) {
        val supportedAttribute = printerAttributes["${attribute.name}-supported"]
        supportedAttribute?.let { checkIfValueIsSupported(printerAttributes, supportedAttribute.name, attribute.value) }
    }

    fun checkIfValueIsSupported(
        printerAttributes: IppAttributesGroup,
        supportedAttributeName: String,
        value: Any
    ) {
        require(printerAttributes.tag == Printer) { "Printer attributes group expected" }
        if (printerAttributes.isEmpty()) return

        if (!supportedAttributeName.endsWith("-supported"))
            throw IppException("attribute name not ending with '-supported'")

        if (value is Collection<*>) { // instead of providing another signature just check collections iteratively
            for (collectionValue in value) {
                checkIfValueIsSupported(printerAttributes, supportedAttributeName, collectionValue!!)
            }
        } else {
            isAttributeValueSupported(printerAttributes, supportedAttributeName, value)
        }
    }

    private fun isAttributeValueSupported(
        printerAttributes: IppAttributesGroup,
        supportedAttributeName: String,
        value: Any
    ): Boolean? {
        val supportedAttribute = printerAttributes[supportedAttributeName] ?: return null
        val attributeValueIsSupported = when (supportedAttribute.tag) {
            IppTag.Boolean -> { // e.g. 'page-ranges-supported'
                supportedAttribute.value as Boolean
            }

            IppTag.Enum, Charset, NaturalLanguage, MimeMediaType, Keyword, Resolution -> when (supportedAttributeName) {
                "media-col-supported" -> with(value as IppCollection) {
                    members.filter { !supportedAttribute.values.contains(it.name) }
                        .forEach { log.warning { "member unsupported: $it" } }
                    // all member names must be supported
                    supportedAttribute.values.containsAll(members.map { it.name })
                }

                else -> supportedAttribute.values.contains(value)
            }

            Integer -> {
                if (supportedAttribute.is1setOf()) supportedAttribute.values.contains(value)
                else value is Int && value <= supportedAttribute.value as Int // e.g. 'job-priority-supported'
            }

            RangeOfInteger -> {
                value is Int && value in supportedAttribute.value as IntRange
            }

            else -> null
        }
        when (attributeValueIsSupported) {
            null -> log.warning { "unable to check if value '$value' is supported by $supportedAttribute" }
            true -> log.fine { "$supportedAttributeName: $value" }
            false -> {
                log.warning { "according to printer attributes value '${supportedAttribute.enumNameOrValue(value)}' is not supported." }
                log.warning { "$supportedAttribute" }
            }
        }
        return attributeValueIsSupported
    }
}