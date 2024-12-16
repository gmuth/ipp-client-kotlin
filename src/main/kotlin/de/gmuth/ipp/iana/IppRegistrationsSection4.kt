package de.gmuth.ipp.iana

import de.gmuth.ipp.core.IppException
import java.util.logging.Logger.getLogger

/**
 * Copyright (c) 2020-2024 Gerhard Muth
 */

// https://www.iana.org/assignments/ipp-registrations/ipp-registrations.xhtml#ipp-registrations-4
object IppRegistrationsSection4 {

    data class KeywordAttributeValue(
        val attribute: String,
        val keywordValue: String,
        val syntax: String,
        val type: String,
        val reference: String
    ) {
        constructor(columns: List<String>) : this(
            attribute = columns[0],
            keywordValue = columns[1],
            syntax = columns[2],
            type = columns[3],
            reference = columns[4]
        )

        override fun toString() = "$attribute $keywordValue ($syntax), $type $reference"
    }

    // source: https://www.iana.org/assignments/ipp-registrations/ipp-registrations-4.csv
    val allKeywordAttributeValuesTable = CSVTable("/ipp-registrations-4.csv", ::KeywordAttributeValue)
    val allKeywordAttributeValues = allKeywordAttributeValuesTable.rows

    fun getKeywordAttributeValuesForAttribute(attribute: String) = allKeywordAttributeValues
        .filter { it.attribute == attribute }
        .apply { if (isEmpty()) throw IppException("Attribute not found: $attribute") }

    fun getKeywordValuesForAttribute(attribute: String) = getKeywordAttributeValuesForAttribute(attribute)
        .filterNot { it.keywordValue.isBlank() || it.keywordValue.contains("Any") }
        .map { it.keywordValue }

    private val logger = getLogger(javaClass.name)

    fun listAllAttributes() = allKeywordAttributeValues
        .map { it.attribute }
        .distinct()
        .groupBy { it.take(7) }
        .forEach { logger.info { it.value.joinToString(", ") } }

    fun listKeywordValuesForAttribute(attribute: String) {
        IppRegistrationsSection2.getAttribute(attribute)?.apply {
            logger.info { "keyword values for $name ($syntax), $collection, $reference}" }
        }
        getKeywordValuesForAttribute(attribute)
            .groupBy { it.take(3) }
            .forEach { logger.info { it.value.joinToString(", ") } }
    }
}