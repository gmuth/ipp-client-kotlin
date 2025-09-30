package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2025 Gerhard Muth
 */

import de.gmuth.ipp.core.IppException.IppAttributeNotFoundException
import java.io.File
import java.io.PrintWriter
import java.io.Writer
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.logging.Level
import java.util.logging.Level.INFO
import java.util.logging.Logger
import java.util.logging.Logger.getLogger
import kotlin.io.path.createDirectories

@Suppress("kotlin:S100")
class IppAttributesGroup(val tag: IppTag) : LinkedHashMap<String, IppAttribute<*>>() {

    private val logger = getLogger(javaClass.name)
    val name = "${tag.name.lowercase()} group"

    companion object {
        var replaceEnabled: Boolean = true
    }

    init {
        require(tag.isGroupTag(), { "'$tag' is not a group tag" })
    }

    fun put(attribute: IppAttribute<*>) =
        if (containsKey(attribute.name)) {
            if (replaceEnabled) { // some implementations do not follow the IPP specification
                put(attribute.name, attribute).also {
                    logger.fine { "$name: '$it' replaced with '${attribute.values.joinToString(",")}'" }
                }
            } else {
                logger.fine { "Ignored replacement attribute: $attribute" }
                null
            }
        } else {
            put(attribute.name, attribute)
        }

    fun attribute(name: String, tag: IppTag, vararg values: Any) =
        IppAttribute(name, tag, values.toList()).also { put(it) }

    fun attribute(name: String, tag: IppTag, values: Collection<Any>) =
        IppAttribute(name, tag, values).also { put(it) }

    @Suppress("UNCHECKED_CAST")
    fun <T> getValueOrNull(name: String): T? = get(name)?.run {
        if (tag.isValueTagAndIsNotOutOfBandTag()) {
            if (values.size == 1) value as T?
            else {
                logger.warning { "For '$name' one value was expected but found ${values.size} values: $values (ignoring all)" }
                null // workaround for https://github.com/gmuth/ipp-client-kotlin/issues/36
            }
        } else null
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

    fun getKeywordOrName(name: String) =
        get(name)?.getKeywordOrName() ?: throwIppAttributeNotFoundException(name)

    fun getKeywordsOrNames(name: String) =
        get(name)?.getKeywordsOrNames() ?: throwIppAttributeNotFoundException(name)

    fun getValueAsZonedDateTime(name: String, zoneId: ZoneId = ZoneId.systemDefault()): ZonedDateTime =
        get(name)?.getValueAsZonedDateTime()?.withZoneSameInstant(zoneId)
            ?: throw IppAttributeNotFoundException(name, tag)

    fun getValueAsDurationOfSeconds(name: String): Duration =
        get(name)?.getValueAsDurationOfSeconds() ?: throwIppAttributeNotFoundException(name)

    private fun throwIppAttributeNotFoundException(attributeName: String): Nothing =
        throw IppAttributeNotFoundException(attributeName, tag)

    fun removeAttributesWhereTagIsNotValueTagOrTagIsOutOfBandTag() = values
        .filter { !it.tag.isValueTagAndIsNotOutOfBandTag() }
        .map { remove(it.name) }
        .onEach { logger.finer { "$name: Removed attribute $it" } }
        .apply { if (isNotEmpty()) logger.fine { "$name: Removed $size attributes  ${joinToString(",") { it!!.name }}" } }

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

    @JvmOverloads
    fun writeText(Writer: Writer, title: String? = toString(), prefix: String = "  ") =
        writeText(PrintWriter(Writer), title, prefix)

    fun saveText(file: File) = file.run {
        printWriter().use { writeText(it, "File: $name", "") }
        logger.info { "Saved $path" }
    }

    fun saveText(path: Path) {
        path.parent?.createDirectories()
        Files.newBufferedWriter(path).use {
            writeText(it, "File: $name", "")
            logger.info { "Saved $path" }
        }
    }
}

fun StringBuilder.appendAttributeIfGroupContainsKey(
    attributesGroup: IppAttributesGroup,
    attributeName: String,
    compactString: Boolean = true,
    separator: String = ", "
) {
    if (attributesGroup.containsKey(attributeName)) {
        append(separator)
        append(attributesGroup[attributeName]!!.run { if (compactString) toCompactString() else toString() })
    }
}