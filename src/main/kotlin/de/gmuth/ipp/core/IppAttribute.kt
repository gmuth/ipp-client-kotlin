package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2025 Gerhard Muth
 */

import de.gmuth.ipp.core.IppTag.*
import de.gmuth.ipp.iana.IppRegistrationsSection2
import de.gmuth.ipp.iana.IppRegistrationsSection2.attributeIs1setOf
import de.gmuth.ipp.iana.IppRegistrationsSection6.getEnumName
import java.nio.charset.Charset
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.logging.Level
import java.util.logging.Level.INFO
import java.util.logging.Logger
import java.util.logging.Logger.getLogger

data class IppAttribute<T>(val name: String, val tag: IppTag) : IppAttributeBuilder {

    private val logger = getLogger(javaClass.name)

    val values: MutableCollection<T> = mutableListOf()

    init {
        require(tag.isValueTag()) { "'$tag' is not a value tag" }
    }

    constructor(name: String, tag: IppTag, values: Collection<T>) : this(name, tag) {
        if (values.isNotEmpty()) tag.validateValueClass(values.first() as Any)
        this.values.addAll(values)
    }

    constructor(name: String, tag: IppTag, vararg values: T) : this(name, tag, values.toList())

    constructor(name: String, vararg values: T) : this(name, NoValue, values.toList()) {
        val tag = IppRegistrationsSection2.tagForAttribute(name) ?: throw IppException("No tag found for '$name'")
        throw IllegalArgumentException("Use constructor with tag: IppAttribute(\"$name\", IppTag.${tag.name}, ...)")
    }

    val value: T
        get() = when {
            attributeIs1setOf(name) == true -> throw IppException("'$name' is registered as '1setOf', use 'values' instead")
            values.size > 1 -> throw IppException("'$name' has ${values.size} values, use 'values' instead")
            values.isEmpty() -> throw IppException("'$name' has no values")
            else -> values.single()
        }

    @Suppress("UNCHECKED_CAST")
    fun additionalValue(attribute: IppAttribute<*>) {
        when {
            attribute.name.isNotEmpty() -> throw IppException("For additional '$name' values attribute name must be empty")
            attribute.values.size != 1 -> throw IppException("Expected 1 additional value, not ${attribute.values.size}")
            attribute.tag != tag -> logger.warning { "$name: ignore additional value \"$attribute\" - tag is not '$tag'" }
            else -> {
                attribute.tag.validateValueClass(attribute.values.single() as Any)
                values.add(attribute.values.single() as T)
            }
        }
    }

    fun is1setOf() = values.size > 1 || attributeIs1setOf(name) == true

    fun isCollection() = tag == BegCollection

    companion object {
        internal fun getStringOrIppStringText(value: Any): String = when (value) {
            is String -> value
            is IppString -> value.text
            else -> throw IllegalArgumentException("Expected String or IppString value but found ${value.javaClass.name}")
        }
    }

    internal fun requireTagKeywordOrName() =
        require(tag in listOf(Keyword, NameWithoutLanguage, NameWithLanguage)) { "Tag is not keyword or name: $tag" }

    fun getKeywordOrName(): String =
        getStringOrIppStringText(value as Any).apply { requireTagKeywordOrName() }

    fun getKeywordsOrNames(): List<String> =
        values.map { getStringOrIppStringText(it as Any) }.apply { requireTagKeywordOrName() }

    override fun buildIppAttribute(printerAttributes: IppAttributesGroup) = this

    fun getValueAsZonedDateTime() = Instant
        .ofEpochSecond((value as Int).toLong())
        .atZone(ZoneOffset.UTC) // epoch always uses UTC

    fun getValueAsDurationOfSeconds(): Duration =
        Duration.ofSeconds((value as Int).toLong())

    override fun toString() = StringBuilder(name).run {
        append(" (${if (is1setOf()) "1setOf " else ""}$tag) = ")
        append(valuesToString())
        toString()
    }

    fun toCompactString() = "$name=${valuesToString(",")}"

    fun valuesToString(separator: String = ", ") =
        if (values.isEmpty()) "no-values"
        else values.joinToString(separator) { valueToString(it) }

    private fun valueToString(value: T) = when {
        tag == Charset -> with(value as Charset) { name().lowercase() }
        tag == RangeOfInteger -> with(value as IntRange) { "$start-$endInclusive" }
        tag == Integer && (name.endsWith("duration")
                || name.endsWith("time-interval")
                || name.endsWith("time-out")
                || name.endsWith("printer-up-time")) -> {
            // should indicate the up-time in seconds (RFC 8011 5.4.29)
            val duration = getValueAsDurationOfSeconds()
            if (duration > Duration.ofDays(10 * 365)) { // looks like epoc time
                "$value (${getValueAsZonedDateTime()})"
            } else {
                "$value ($duration)"
            }
        }

        tag == Integer && (name.endsWith("time") || name.startsWith("time")) ->
            "$value (${getValueAsZonedDateTime()})"

        value is ByteArray -> with(value) { if (isEmpty()) "" else "$size bytes" }
        else -> enumNameOrValue(value as Any).toString()
    }

    fun enumNameOrValue(value: Any) =
        if (tag == IppTag.Enum) getEnumName(name, value) else value

    fun log(logger: Logger, level: Level = INFO, prefix: String = "") = logger.also {
        val string = toString()
        if (string.length < 160) {
            it.log(level) { "$prefix$string" }
        } else {
            it.log(level) { "$prefix$name ($tag) =" }
            for (value in values) {
                if (value is IppCollection) {
                    (value as IppCollection).log(logger, level, "$prefix  ")
                } else {
                    it.log(level) { "$prefix  ${enumNameOrValue(value as Any)}" }
                }
            }
        }
    }
}