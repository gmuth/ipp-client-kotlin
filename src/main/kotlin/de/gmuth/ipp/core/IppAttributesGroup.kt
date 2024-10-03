package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2024 Gerhard Muth
 */

import de.gmuth.ipp.core.IppException.IppAttributeNotFoundException
import java.io.File
import java.io.PrintWriter
import java.net.URI
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.logging.Level
import java.util.logging.Level.INFO
import java.util.logging.Logger
import java.util.logging.Logger.getLogger

class IppAttributesGroup(val tag: IppTag) : LinkedHashMap<String, IppAttribute<*>>() {

    private val logger = getLogger(javaClass.name)
    val name = "${tag.name.lowercase()} group"

    companion object {
        var replaceEnabled: Boolean = true
    }

    init {
        require(tag.isGroupTag(), { "'$tag' is not a group tag" })
    }

    fun put(attribute: IppAttribute<*>) {
        if (containsKey(attribute.name)) {
            if (replaceEnabled) { // some implementations do not follow the IPP specification
                put(attribute.name, attribute).also {
                    logger.fine { "replaced '$it' with '${attribute.values.joinToString(",")}' in $name" }
                }
            } else {
                logger.fine { "ignored replacement attribute: $attribute" }
            }
        } else {
            put(attribute.name, attribute)
        }
    }

    fun attribute(name: String, tag: IppTag, vararg values: Any) =
        put(IppAttribute(name, tag, values.toList()))

    fun attribute(name: String, tag: IppTag, values: Collection<Any>) =
        put(IppAttribute(name, tag, values))

    @Suppress("UNCHECKED_CAST")
    fun <T> getValueOrNull(name: String): T? = get(name)?.run {
        if (tag.isValueTagAndIsNotOutOfBandTag()) value as T?
        else null
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getValuesOrNull(name: String): T? = get(name)?.run {
        if (tag.isValueTagAndIsNotOutOfBandTag()) values as T?
        else null
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getValue(name: String): T = get(name)?.run {
        if (tag.isValueTagAndIsNotOutOfBandTag()) value as T
        else throw IppException("'$name' value is out-of-band: tag=$tag")
    } ?: throwIppAttributeNotFoundException(name)

    @Suppress("UNCHECKED_CAST")
    fun <T> getValues(name: String): T = get(name)?.run {
        if (tag.isValueTagAndIsNotOutOfBandTag()) values as T
        else throw IppException("'$name' values are out-of-band: tag=$tag")
    } ?: throwIppAttributeNotFoundException(name)

    fun getValueAsURI(name: String) =
        getValue<URI>(name)

    fun getValueAsString(name: String) =
        get(name)?.getStringValue() ?: throwIppAttributeNotFoundException(name)

    fun getValuesAsListOfStrings(name: String) =
        get(name)?.getStringValues() ?: throwIppAttributeNotFoundException(name)

    fun getValueAsZonedDateTime(name: String, zoneId: ZoneId = ZoneId.systemDefault()): ZonedDateTime =
        get(name)?.getValueAsZonedDateTime()?.withZoneSameInstant(zoneId)
            ?: throw IppAttributeNotFoundException(name, tag)

    fun getValueAsDurationOfSeconds(name: String): Duration =
        get(name)?.getValueAsDurationOfSeconds() ?: throwIppAttributeNotFoundException(name)

    private fun throwIppAttributeNotFoundException(attributeName: String): Nothing =
        throw IppAttributeNotFoundException(attributeName, tag)

    fun `remove attributes where tag is not ValueTag or tag is OutOfBandTag`() = values
        .filter { !it.tag.isValueTagAndIsNotOutOfBandTag() }
        .map { remove(it.name) }
        .onEach { logger.finer { "removed attribute from $name: $it" } }
        .apply { if (isNotEmpty()) logger.fine { "removed $size attributes from $name: ${joinToString(",") { it!!.name }}" } }

    fun put(attributesGroup: IppAttributesGroup) =
        attributesGroup.values.forEach { put(it) }

    override fun toString() = "$name ($size attributes)"

    fun toValuesString() =
        values.joinToString(" ") { it.valuesToString() }

    fun toCompactString() =
        values.joinToString(" ") { it.toCompactString() }

    @JvmOverloads
    fun log(logger: Logger, level: Level = INFO, title: String = toString(), prefix: String = "") {
        logger.log(level) { "${prefix}$title" }
        keys.forEach { logger.log(level) { "$prefix  ${get(it)}" } }
    }

    @JvmOverloads
    fun writeText(printWriter: PrintWriter, title: String? = toString(), prefix: String = "  ") = printWriter.run {
        title?.let { println(it) }
        values.forEach { println("$prefix$it") }
    }

    fun saveText(file: File) = file.apply {
        printWriter().use { writeText(it, "# File: ${file.name}", "") }
        logger.info { "Saved $path" }
    }
}