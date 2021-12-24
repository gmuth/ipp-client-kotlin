package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2021 Gerhard Muth
 */

import de.gmuth.ipp.core.IppTag.*
import de.gmuth.ipp.iana.IppRegistrationsSection2.attributeIs1setOf
import de.gmuth.ipp.iana.IppRegistrationsSection6.getEnumName
import de.gmuth.log.Logging
import java.nio.charset.Charset

data class IppAttribute<T> constructor(val name: String, val tag: IppTag) : IppAttributeBuilder {

    companion object {
        val log = Logging.getLogger {}
    }

    val values = mutableListOf<T>()

    init {
        if (tag.isDelimiterTag()) throw IppException("'$tag' is not a value tag")
    }

    constructor(name: String, tag: IppTag, values: Collection<T>) : this(name, tag) {
        if (values.isNotEmpty()) validateValueClass(values.first() as Any)
        this.values.addAll(values.toList())
    }

    constructor(name: String, tag: IppTag, vararg values: T) : this(name, tag, values.toList())

    val value: T
        get() = values.single().apply {
            if (attributeIs1setOf(name) == true) log.warn { "'$name' is registered as '1setOf', use 'values' instead" }
        }

    @Suppress("UNCHECKED_CAST")
    fun additionalValue(attribute: IppAttribute<*>): Any = when {
        attribute.name.isNotEmpty() -> throw IppException("for additional '$name' values attribute name must be empty")
        attribute.values.size != 1 -> throw IppException("expected 1 additional value, not ${attribute.values.size}")
        attribute.tag != tag -> log.warn { "$name: ignore additional value \"$attribute\" - tag is not '$tag'" }
        else -> {
            validateValueClass(attribute.value as Any)
            values.add(attribute.value as T)
        }
    }

    internal fun validateValueClass(value: Any) {
        if (!tag.valueHasValidClass(value)) throw IppException("value class ${value::class.java.name} not valid for tag $tag")
    }

    fun is1setOf() = values.size > 1 || attributeIs1setOf(name) == true

    fun isCollection() = tag == BegCollection

    override fun buildIppAttribute(printerAttributes: IppAttributesGroup): IppAttribute<*> = this

    override fun toString(): String {
        val tagString = "${if (is1setOf()) "1setOf " else ""}$tag"
        val valuesString = if (values.isEmpty()) "no-value" else values.joinToString(",") { valueToString(it) }
        return "$name ($tagString) = $valuesString"
    }

    internal fun valueToString(value: T) = when {
        tag == Charset -> with(value as Charset) { name().lowercase() }
        tag == RangeOfInteger -> with(value as IntRange) { "$start-$endInclusive" }
        value is ByteArray -> with(value as ByteArray) { if (isEmpty()) "no-value" else "$size bytes" }
        else -> enumNameOrValue(value as Any).toString()
    }

    fun enumNameOrValue(value: Any) = if (tag == IppTag.Enum) getEnumName(name, value) else value

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