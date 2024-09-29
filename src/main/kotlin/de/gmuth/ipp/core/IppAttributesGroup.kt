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

    companion object {
        var replaceEnabled: Boolean = true
    }

    init {
        require(tag.isGroupTag(), { "'$tag' is not a group tag" })
    }

    fun put(attribute: IppAttribute<*>) {
        if (containsKey(attribute.name)) {
            // some implementations do not follow the IPP specification
            if (replaceEnabled) {
                put(attribute.name, attribute).also {
                    logger.fine { "replaced '$it' with '${attribute.values.joinToString(",")}' in group $tag" }
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
    fun <T> getValueOrNull(name: String) =
        get(name)?.value as T?

    @Suppress("UNCHECKED_CAST")
    fun <T> getValuesOrNull(name: String) =
        get(name)?.values as T?

    @Suppress("UNCHECKED_CAST")
    fun <T> getValue(name: String) =
        get(name)?.value as T ?: throw IppAttributeNotFoundException(name, tag)

    @Suppress("UNCHECKED_CAST")
    fun <T> getValues(name: String) =
        get(name)?.values as T ?: throw IppAttributeNotFoundException(name, tag)

    fun getValueAsURI(name: String) =
        getValue<URI>(name)

    fun getValueAsString(name: String) =
        getValue<IppString>(name).text

    fun getValuesAsListOfStrings(name: String) =
        get(name)?.getStringValues() ?: throw IppAttributeNotFoundException(name, tag)

    fun getValueAsZonedDateTime(name: String, zoneId: ZoneId = ZoneId.systemDefault()): ZonedDateTime =
        get(name)?.getValueAsZonedDateTime()?.withZoneSameInstant(zoneId)
            ?: throw IppAttributeNotFoundException(name, tag)

    fun getValueAsDurationOfSeconds(name: String): Duration =
        get(name)?.getValueAsDurationOfSeconds() ?: throw IppAttributeNotFoundException(name, tag)

    fun put(attributesGroup: IppAttributesGroup) =
        attributesGroup.values.forEach { put(it) }

    override fun toString() = "'$tag' $size attributes"

    fun toValuesString() =
        values.joinToString(" ") { it.valuesToString() }

    fun toCompactString() =
        values.joinToString(" ") { it.toCompactString() }

    @JvmOverloads
    fun log(logger: Logger, level: Level = INFO, prefix: String = "", title: String = "$tag") {
        logger.log(level) { "${prefix}$title" }
        keys.forEach { logger.log(level) { "$prefix  ${get(it)}" } }
    }

    @JvmOverloads
    fun writeText(printWriter: PrintWriter, title: String? = "$tag", prefix: String = "  ") = printWriter.run {
        title?.let { println(it) }
        values.forEach { println("$prefix$it") }
    }

    fun saveText(file: File) = file.apply {
        printWriter().use { writeText(it, "# File: ${file.name}", "") }
        logger.info { "Saved $path" }
    }
}