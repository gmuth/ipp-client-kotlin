package de.gmuth.ipp.iana

import de.gmuth.csv.CSVReader
import de.gmuth.csv.CSVReader.RowMapper
import de.gmuth.ipp.core.IppOperation

/**
 * https://www.iana.org/assignments/ipp-registrations/ipp-registrations.xhtml#ipp-registrations-6
 */
class IppRegistrationsSection6 {

    data class EnumAttributeValue(
            val attribute: String,
            val value: String,
            val name: String,
            val syntax: String,
            val reference: String
    ) {

        companion object {
            val rowMapper = object : RowMapper<EnumAttributeValue> {
                override fun mapRow(columns: List<String>, rowNum: Int) =
                        EnumAttributeValue(
                                attribute = columns[0],
                                value = columns[1],
                                name = columns[2],
                                syntax = columns[3],
                                reference = columns[4]
                        )
            }
        }

        override fun toString() = "$attribute/$value ($syntax) = $name $reference "
    }

    companion object {

        // source: https://www.iana.org/assignments/ipp-registrations/ipp-registrations-6.csv
        val allEnumAttributeValues =
                CSVReader(EnumAttributeValue.rowMapper).readResource("/ipp-registrations-6.csv", true)

        val enumAttributeValuesMap =
                allEnumAttributeValues.associateBy { "${it.attribute}/${it.value}" }

        // alias example: finishings-default, <Any "finishings" value>
        val aliasMap = mutableMapOf<String, String>().apply {
            allEnumAttributeValues
                    .filter { it.value.toLowerCase().contains("any") }
                    .forEach { put(it.attribute, it.value.replace("^.*\"(.+)\".*$".toRegex(), "$1")) }
        }

        fun getEnumAttributeValue(attribute: String, value: Any) =
                enumAttributeValuesMap.get("$attribute/$value")

        fun getEnumName(attribute: String, value: Any) =
                if (attribute == "operations-supported" && value is Int) {
                    getOperationsSupportedValueName(value)
                } else {
                    getEnumAttributeValue(aliasMap[attribute] ?: attribute, value)?.name
                } ?: value

        fun getOperationsSupportedValueName(value: Int): Any? =
                if (value < 0x4000) {
                    getEnumAttributeValue("operations-supported", String.format("0x%04X", value))?.name
                } else {
                    // CUPS operations are not iana registered
                    IppOperation.fromShort(value.toShort()).toString()
                }
    }

}