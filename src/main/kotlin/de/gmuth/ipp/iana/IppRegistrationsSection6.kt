package de.gmuth.ipp.iana

import de.gmuth.csv.CSVTable
import de.gmuth.log.Logging

/**
 * https://www.iana.org/assignments/ipp-registrations/ipp-registrations.xhtml#ipp-registrations-6
 */
object IppRegistrationsSection6 {

    data class EnumAttributeValue(
            val attribute: String,
            val value: String,
            val name: String,
            val syntax: String,
            val reference: String
    ) {
        constructor(columns: List<String>) : this(
                attribute = columns[0],
                value = columns[1],
                name = columns[2],
                syntax = columns[3],
                reference = columns[4]
        )

        override fun toString() = "$attribute/$value ($syntax) = $name $reference "
    }

    val log = Logging.getLogger {}

    // source: https://www.iana.org/assignments/ipp-registrations/ipp-registrations-6.csv
    val allEnumAttributeValues = CSVTable("/ipp-registrations-6.csv", ::EnumAttributeValue).rows

    val enumAttributeValuesMap = allEnumAttributeValues.associateBy { "${it.attribute}/${it.value}" }

    // alias example: finishings-default, <Any "finishings" value>
    val aliasMap = mutableMapOf<String, String>().apply {
        allEnumAttributeValues
                .filter { it.value.lowercase().contains("any") }
                .forEach { put(it.attribute, it.value.replace("^.*\"(.+)\".*$".toRegex(), "$1")) }
    }

    fun getEnumAttributeValue(attribute: String, value: Any) = enumAttributeValuesMap.get("$attribute/$value")

    fun getEnumName(attribute: String, value: Any) =
            if (attribute == "operations-supported" && value is Number) {
                // lookup the name in IppOperation because CUPS operations are not iana registered
                de.gmuth.ipp.core.IppOperation.fromShort(value.toShort()).toString()
            } else {
                getEnumAttributeValue(aliasMap[attribute] ?: attribute, value)?.name
            } ?: value

}