package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2022 Gerhard Muth
 */

import de.gmuth.log.Logging
import java.io.File

open class IppAttributesGroup(val tag: IppTag) : LinkedHashMap<String, IppAttribute<*>>() {

    companion object {
        val log = Logging.getLogger {}
    }

    init {
        if (!tag.isGroupTag()) throw IppException("'$tag' is not a group tag")
    }

    open fun put(attribute: IppAttribute<*>, onReplaceWarn: Boolean = false) =
        put(attribute.name, attribute).apply {
            if (this != null && onReplaceWarn) log.warn { "replaced '$this' with '${attribute.values.joinToString(",")}' in group $tag" }
        }

    fun attribute(name: String, tag: IppTag, vararg values: Any) =
        put(IppAttribute(name, tag, values.toList()))

    fun attribute(name: String, tag: IppTag, values: List<Any>) =
        put(IppAttribute(name, tag, values))

    @Suppress("UNCHECKED_CAST")
    fun <T> getValueOrNull(name: String) =
        get(name)?.value as T?

    @Suppress("UNCHECKED_CAST")
    fun <T> getValue(name: String) =
        get(name)?.value as T ?: throw IppException("attribute '$name' not found in group $tag")

    @Suppress("UNCHECKED_CAST")
    fun <T> getValues(name: String) =
        get(name)?.values as T ?: throw IppException("attribute '$name' not found in group $tag")

    fun put(attributesGroup: IppAttributesGroup) {
        log.debug { "put ${attributesGroup.size} attributes" }
        attributesGroup.values.forEach { put(it, false) }
    }

    override fun toString() = "'$tag' $size attributes"

    @JvmOverloads
    fun logDetails(prefix: String = "", title: String = "$tag") {
        log.info { "${prefix}$title" }
        keys.forEach { log.info { "$prefix  ${get(it)}" } }
    }

    fun saveText(file: File) = file.apply {
        bufferedWriter().use {
            values.forEach { value ->
                it.write(value.toString())
                it.newLine()
            }
        }
        log.info { "saved: $path" }
    }

}