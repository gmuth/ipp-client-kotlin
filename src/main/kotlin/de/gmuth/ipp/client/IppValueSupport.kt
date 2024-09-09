package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2024 Gerhard Muth
 */

import de.gmuth.ipp.core.*
import de.gmuth.ipp.core.IppTag.*
import java.util.logging.Logger.getLogger

// ------------------------------------------------------
// Attribute value checking based on printer capabilities
// ------------------------------------------------------

object IppValueSupport {

    private val logger = getLogger(javaClass.name)

    fun checkIfValueIsSupported(
        printerAttributes: IppAttributesGroup, attribute: IppAttribute<*>,
        throwIfSupportedAttributesIsNotAvailable: Boolean
    ) {
        val supportedAttribute = printerAttributes["${attribute.name}-supported"]
        if (supportedAttribute == null) logger.warning { "${attribute.name}-supported not available in printer attributes" }
        else checkIfValueIsSupported(printerAttributes, attribute.name, attribute.value as Any, throwIfSupportedAttributesIsNotAvailable)
    }

    fun checkIfValueIsSupported(
        printerAttributes: IppAttributesGroup,
        attributeName: String,
        value: Any,
        throwIfSupportedAttributesIsNotAvailable: Boolean
    ) {
        require(printerAttributes.tag == Printer) { "Printer attributes group expected" }
        if (printerAttributes.isEmpty()) return

        if (value is Collection<*>) { // instead of providing another signature just check collections iteratively
            for (collectionValue in value) {
                checkIfValueIsSupported(printerAttributes, attributeName, collectionValue!!, throwIfSupportedAttributesIsNotAvailable)
            }
        } else {
            val supportedAttributeName = "$attributeName-supported"
            if(!printerAttributes.containsKey(supportedAttributeName) && throwIfSupportedAttributesIsNotAvailable)
                throw IppException("Unable to check value '$value' because printer attribute '$supportedAttributeName' is not available.")
            isAttributeValueSupported(printerAttributes, attributeName, value)
        }
    }

    private fun isAttributeValueSupported(
        printerAttributes: IppAttributesGroup,
        attributeName: String,
        value: Any
    ): Boolean? {
        val supportedAttributeName = "$attributeName-supported"
        val supportedAttribute = printerAttributes[supportedAttributeName] ?: return null
        val attributeValueIsSupported = when (supportedAttribute.tag) {
            IppTag.Boolean -> { // e.g. 'page-ranges-supported'
                supportedAttribute.value as Boolean
            }

            IppTag.Enum, Charset, NaturalLanguage, MimeMediaType, Keyword, Resolution -> when (supportedAttributeName) {
                "media-col-supported" -> with(value as IppCollection) {
                    members
                        .onEach { checkIfValueIsSupported(printerAttributes, it, false) }
                        .filter { !supportedAttribute.values.contains(it.name) }
                        .forEach { logger.warning { "media-col member unsupported: $it" } }
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
            null -> logger.warning { "Unable to check if value '$value' is supported by $supportedAttribute" }
            true -> logger.finer { "$value is supported according to $supportedAttributeName" }
            false -> {
                logger.warning { "According to printer attributes value '${supportedAttribute.enumNameOrValue(value)}' is not supported for attribute '$attributeName'." }
                logger.warning { "$supportedAttribute" }
            }
        }
        return attributeValueIsSupported
            .also { logger.finest { "is $supportedAttributeName(${supportedAttribute.tag})? $value -> $attributeValueIsSupported" } }
    }
}