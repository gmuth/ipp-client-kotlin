package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.iana.IppRegistrationsSection2
import de.gmuth.ipp.iana.IppRegistrationsSection6
import java.nio.charset.Charset
import java.util.*

class IppAttribute<T> constructor(val name: String, val tag: IppTag) : IppAttributeHolder {

    val values = mutableListOf<T>()

    init {
        if (tag.isDelimiterTag()) {
            throw IppException("delimiter tag '$tag' must not be used for attributes")
        }
    }

    companion object {
        var allowAutomaticTag: Boolean = true
    }

    constructor(name: String, tag: IppTag, vararg values: T) : this(name, tag, values.toList())

    constructor(name: String, tag: IppTag, values: Collection<T>) : this(name, tag) {
        this.values.addAll(values)
    }

    // automatic tag

    constructor(name: String, vararg values: T) : this(name, values.toList())

    constructor(name: String, values: Collection<T>) : this(
            name,
            IppRegistrationsSection2.tagForAttribute(name) ?: throw IppException("no tag found for attribute '$name'"),
            values
    ) {
        if (!allowAutomaticTag) {
            throw IppException("automatic tag disabled: for attribute '$name' use IppTag.${tag.name}")
        }
    }

    override fun getIppAttribute(printerAttributes: IppAttributesGroup): IppAttribute<*> = this

    fun validateTag() {
        val registeredTag = IppRegistrationsSection2.tagForAttribute(name)
        if (registeredTag != null && tag != registeredTag) {
            throw IppException("expected tag '$registeredTag' for '$name' but found tag: '$tag'")
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun additionalValue(attribute: IppAttribute<*>) {
        when {
            attribute.name.isNotEmpty() -> throw IppException("name must be empty for additional values")
            attribute.values.size != 1 -> throw IppException("expected 1 additional value, not ${attribute.values.size}")
            attribute.values.first() == Unit -> throw IppException("expected a value, not 'Unit'")
            tag != attribute.tag -> throw IppException("expected tag '$tag' for additional value but found '${attribute.tag}'")
            else -> values.add(attribute.value as T)
        }
    }

    fun is1setOf() = values.size > 1 || IppRegistrationsSection2.attributeIs1setOf(name) == true

    val value: T
        get() {
            if (IppRegistrationsSection2.attributeIs1setOf(name) == true) {
                println("WARN: '$name' is registered as '1setOf', use 'values' instead")
            }
            if (values.size > 1) {
                throw IppException("found ${values.size.toPluralString("value")} but expected 0 or 1 for '$name'")
            }
            return values.first()
        }

    fun enumValueNameOrValue(value: Any) = when (tag) {
        IppTag.Enum -> IppRegistrationsSection6.getEnumValueName(name, value)
        else -> value
    }

    override fun toString(): String {
        val tagString = "${if (is1setOf()) "1setOf " else ""}$tag"
        return "$name ($tagString) = ${valuesToString()}"
    }

    private fun valuesToString(): String =
            if (values.isEmpty()) {
                "no-value"
            } else {
                values.joinToString(",") {
                    when {
                        tag == IppTag.Charset -> with(it as Charset) {
                            name().toLowerCase()
                        }
                        tag == IppTag.NaturalLanguage -> with(it as Locale) {
                            toLanguageTag().toLowerCase()
                        }
                        tag == IppTag.RangeOfInteger -> with(it as IntRange) {
                            "$start-$endInclusive"
                        }
                        tag == IppTag.Integer && name.contains("time") && !name.contains("time-out") -> with(it as Int) {
                            IppIntegerTime.fromInt(this).toString()
                        }
                        else -> with(it as Any) {
                            enumValueNameOrValue(this).toString()
                        }
                    }
                }
            }

    fun logDetails(prefix: String = "") {
        val string = toString()
        if (string.length < 160) {
            println("$prefix$string")
        } else {
            println("$prefix$name ($tag) =")
            for (value in values) {
                if (value is IppCollection) {
                    value.logDetails("$prefix  ")
                } else {
                    println("${prefix}  ${enumValueNameOrValue(value as Any)}")
                }
            }
        }
    }
}