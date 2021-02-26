package de.gmuth.ipp.core

import de.gmuth.log.Logging

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class IppAttributesGroup(val tag: IppTag, private val replacementAllowed: Boolean = true) : LinkedHashMap<String, IppAttribute<*>>() {

    companion object {
        val log = Logging.getLogger {}
    }

    init {
        if (!tag.isGroupTag()) throw IppException("'$tag' is not a valid group tag")
    }

    fun put(attribute: IppAttribute<*>): IppAttribute<*>? {
        attribute.checkSyntax()
        if (attribute.values.isEmpty() && !attribute.tag.isOutOfBandTag()) {
            throw IllegalArgumentException("empty value list")
        }
        return if (containsKey(attribute.name) && !replacementAllowed) {
            log.warn { "replacement denied for '$attribute' in group $tag" }
            get(attribute.name)
        } else {
            val replaced = put(attribute.name, attribute)
            if (replaced != null) {
                log.warn { "replaced '$replaced' with '${attribute.values.joinToString(",")}' in group $tag" }
            }
            replaced
        }
    }

    fun attribute(name: String, tag: IppTag, vararg values: Any) =
            put(IppAttribute(name, tag, values.toMutableList()))

    fun attribute(name: String, tag: IppTag, values: List<Any>) =
            put(IppAttribute(name, tag, values.toMutableList()))

    @Suppress("UNCHECKED_CAST")
    fun <T> getValue(name: String) =
            get(name)?.value as T ?: throw NoSuchElementException("'$name' not in group $tag")

    @Suppress("UNCHECKED_CAST")
    fun <T> getValues(name: String) =
            get(name)?.values as T ?: throw NoSuchElementException("'$name' not in group $tag")

    override fun toString() = "'$tag' $size attributes"

    @JvmOverloads
    fun logDetails(prefix: String = "", title: String = "$tag") {
        log.info { "${prefix}$title" }
        for (key in keys) {
            log.info { "$prefix  ${get(key)}" }
        }
    }

}