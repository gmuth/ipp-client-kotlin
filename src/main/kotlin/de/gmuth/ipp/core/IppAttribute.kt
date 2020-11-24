package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ext.toPluralString
import de.gmuth.ipp.iana.IppRegistrationsSection2
import de.gmuth.ipp.iana.IppRegistrationsSection6
import de.gmuth.log.Log
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

open class IppAttribute<T> constructor(val name: String, val tag: IppTag) : IppAttributeBuilder {

    val values = mutableListOf<T>()

    init {
        if (tag.isDelimiterTag()) throw IppException("delimiter tag '$tag' must not be used for attributes")
    }

    companion object {
        val log = Log.getWriter("IppAttribute", Log.Level.WARN)
        var checkSyntaxEnabled: Boolean = true
        var allowAutomaticTag: Boolean = true
        val iso8601DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
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
        if (!allowAutomaticTag) throw IppException("automatic tag disabled: for attribute '$name' use IppTag.${tag.name}")
    }

    override fun buildIppAttribute(printerAttributes: IppAttributesGroup): IppAttribute<*> = this

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
                log.warn { "'$name' is registered as '1setOf', use 'values' instead" }
            }
            if (values.size > 1) {
                throw IppException("found ${values.size.toPluralString("value")} but expected 0 or 1 for '$name'")
            }
            return values.first()
        }

    fun checkSyntax() {
        if (checkSyntaxEnabled) {
            IppRegistrationsSection2.checkSyntaxOfAttribute(name, tag)
            if (values.size > 1 && IppRegistrationsSection2.attributeIs1setOf(name) == false) {
                log.warn { "'$name' is not registered as '1setOf'" }
            }
        }
    }

    fun assertNoValues() {
        if (values.isNotEmpty()) throw IppException("'$name' must not have any value")
    }

    fun assertValuesExist() {
        if (values.isEmpty()) throw IppException("'$name' has no values")
    }

    override fun toString(): String {
        val tagString = "${if (is1setOf()) "1setOf " else ""}$tag"
        return "$name ($tagString) = ${valuesToString()}"
    }

    private fun valuesToString() =
            if (values.isEmpty()) "no-value"
            else values.joinToString(",") { valueToString(it) }

    private fun valueToString(value: T) = when {
        tag == IppTag.Charset -> with(value as Charset) {
            name().toLowerCase()
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
            enumValueNameOrValue(this).toString()
        }
    }

    fun enumValueNameOrValue(value: Any) =
            if (tag == IppTag.Enum) IppRegistrationsSection6.getEnumValueName(name, value) else value

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
                    log.info { "${prefix}  ${enumValueNameOrValue(value as Any)}" }
                }
            }
        }
    }
}