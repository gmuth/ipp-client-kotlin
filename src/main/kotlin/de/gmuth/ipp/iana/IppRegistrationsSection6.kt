package de.gmuth.ipp.iana

import de.gmuth.csv.CSVReader
import de.gmuth.csv.CSVReader.RowMapper

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
        val allEnumAttributeValues = CSVReader(EnumAttributeValue.rowMapper)
                .readResource("/ipp-registrations-6.csv", true)

        val enumAttributeValuesMap = allEnumAttributeValues.associateBy { "${it.attribute}/${it.value}" }

        // alias example: finishings-default, <Any "finishings" value>
        val aliasMap = mutableMapOf<String, String>().apply {
            allEnumAttributeValues
                    .filter { it.value.toLowerCase().contains("any") }
                    .forEach { put(it.attribute, it.value.replace("^.*\"(.+)\".*$".toRegex(), "$1")) }
        }

        fun getEnumAttributeValue(attribute: String, value: Any) = enumAttributeValuesMap.get("$attribute/$value")

        fun getEnumValueName(attribute: String, value: Any): Any {
            val enumAttributeValue = getEnumAttributeValue(
                    aliasMap[attribute] ?: attribute,
                    if (attribute == "operations-supported") String.format("0x%04X", value) else value
            )
            return enumAttributeValue?.name ?: value
        }

    }
}

fun main() {
    CSVReader.prettyPrintResource("/ipp-registrations-6.csv")
    for (enumAttributeValue in IppRegistrationsSection6.allEnumAttributeValues) {
        println(enumAttributeValue)
    }
    println(IppRegistrationsSection6.getEnumValueName("job-state", 9)) // 'completed'
    println(IppRegistrationsSection6.getEnumValueName("finishings", 4)) // 'staple'
    println(IppRegistrationsSection6.getEnumValueName("finishings-supported", 4)) // 'staple'
    println(IppRegistrationsSection6.aliasMap["finishings-supported"]) // 'finishings'
}