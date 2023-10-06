package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import java.io.BufferedWriter
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.logging.Level
import java.util.logging.Level.INFO
import java.util.logging.Logger
import java.util.logging.Logger.getLogger

class IppAttributesGroup(val tag: IppTag) : LinkedHashMap<String, IppAttribute<*>>() {

    private val log = getLogger(javaClass.name)
    var onReplaceWarn: Boolean = false

    init {
        if (!tag.isGroupTag()) throw IppException("'$tag' is not a group tag")
    }

    fun put(attribute: IppAttribute<*>) = put(attribute.name, attribute).also {
        if (it != null && onReplaceWarn) {
            log.warning { "replaced '$it' with '${attribute.values.joinToString(",")}' in group $tag" }
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
        get(name)?.value as T ?: throw IppException("Attribute '$name' not found in group $tag")

    @Suppress("UNCHECKED_CAST")
    fun <T> getValues(name: String) =
        get(name)?.values as T ?: throw IppException("Attribute '$name' not found in group $tag")

    fun getTextValue(name: String) =
        getValue<IppString>(name).text

    fun getTimeValue(name: String): ZonedDateTime =
        Instant.ofEpochSecond(getValue<Int>(name).toLong()).atZone(ZoneOffset.UTC)

    fun put(attributesGroup: IppAttributesGroup) =
        attributesGroup.values.forEach { put(it) }

    override fun toString() = "'$tag' $size attributes"

    @JvmOverloads
    fun log(logger: Logger, level: Level = INFO, prefix: String = "", title: String = "$tag") {
        logger.log(level) { "${prefix}$title" }
        keys.forEach { logger.log(level) { "$prefix  ${get(it)}" } }
    }

    fun write(bufferedWriter: BufferedWriter, title: String = "$tag") = bufferedWriter.run {
        write(title)
        newLine()
        values.forEach {
            write("  $it")
            newLine()
        }
    }

    fun saveText(file: File) = file.apply {
        bufferedWriter().use { write(it) }
        log.info { "Saved $path" }
    }

}