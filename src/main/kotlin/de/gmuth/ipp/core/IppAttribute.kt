package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.iana.IppRegistrationsSection2
import de.gmuth.ipp.iana.IppRegistrationsSection6
import de.gmuth.log.Logging
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

data class IppAttribute<T> constructor(val name: String, val tag: IppTag) : IppAttributeBuilder {

    companion object {
        val log = Logging.getLogger {}
        var validateValueClass = true
        var checkSyntax: Boolean = true
        val iso8601DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    }

    val values = mutableListOf<T>()

    init {
        if (tag.isDelimiterTag()) {
            log.error { "delimiter tag '$tag' must not be used for attributes" }
            throw IllegalArgumentException("$tag")
        }
    }

    constructor(name: String, tag: IppTag, values: Collection<T>) : this(name, tag) {
        if (validateValueClass && values.isNotEmpty()) {
            val firstValue = values.first() as Any
            if (!tag.validateClass(firstValue)) throw IllegalArgumentException("${firstValue::class.java} illegal for tag $tag")
        }
        this.values.addAll(values)
    }

    constructor(name: String, tag: IppTag, vararg values: T) : this(name, tag, values.toList())

    val value: T
        get() {
            if (IppRegistrationsSection2.attributeIs1setOf(name) == true) {
                log.warn { "'$name' is registered as '1setOf', use 'values' instead" }
            }
            return values.single()
        }

    @Suppress("UNCHECKED_CAST")
    fun additionalValue(attribute: IppAttribute<*>) =
            when {
                attribute.tag != tag -> throw IppException("expected additional value with tag '$tag' but found $attribute")
                attribute.values.size != 1 -> throw IppException("expected 1 additional value, not ${attribute.values.size}")
                attribute.name.isNotEmpty() -> throw IppException("for additional '$name' values attribute name must be empty")
                else -> values.add(attribute.value as T)
            }

    fun is1setOf() =
            values.size > 1 || IppRegistrationsSection2.attributeIs1setOf(name) == true

    fun checkSyntax() {
        if (checkSyntax) {
            IppRegistrationsSection2.checkSyntaxOfAttribute(name, tag)
        }
    }

    override fun buildIppAttribute(printerAttributes: IppAttributesGroup): IppAttribute<*> = this

    override fun toString(): String {
        val tagString = "${if (is1setOf()) "1setOf " else ""}$tag"
        val valuesString =
                if (values.isEmpty()) {
                    "no-value"
                } else {
                    values.joinToString(",") { valueToString(it) }
                }
        return "$name ($tagString) = $valuesString"
    }

    private fun valueToString(value: T) = when {
        tag == IppTag.Charset -> with(value as Charset) {
            name().lowerCase()
        }
        tag == IppTag.RangeOfInteger -> with(value as IntRange) {
            "$start-$endInclusive"
        }
        tag == IppTag.Integer && name.contains("time") && !name.contains("time-out") -> with(value as Int) {
            val seconds = value.toLong() // some printers use 'seconds since startup'
            val epochSeconds = if (seconds < 60 * 60 * 24 * 365) Date().time / 1000 - seconds else seconds
            "$value (${iso8601DateFormat.format(Date(epochSeconds * 1000))})"
        }
        else -> with(value as Any) {
            enumNameOrValue(this).toString()
        }
    }

    fun enumNameOrValue(value: Any) =
            if (tag == IppTag.Enum) {
                IppRegistrationsSection6.getEnumName(name, value)
            } else {
                value
            }

    fun logDetails(prefix: String = "") {
        val string = toString()
        if (string.length < 160) {
            log.info { "$prefix$string" }
        } else {
            log.info { "$prefix$name ($tag) =" }
            for (value in values) {
                if (value is IppCollection) {
                    value.logDetails("$prefix  ")
                } else {
                    log.info { "${prefix}  ${enumNameOrValue(value as Any)}" }
                }
            }
        }
    }

}