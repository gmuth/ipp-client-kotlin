package de.gmuth.ipp.core

import de.gmuth.ext.toPluralString
import de.gmuth.log.Log

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class IppAttributesGroup(val tag: IppTag) : LinkedHashMap<String, IppAttribute<*>>() {

    companion object {
        val log = Log.getWriter("IppAttributesGroup", Log.Level.INFO)
    }

    init {
        if (!tag.isDelimiterTag() || tag.isEndTag()) throw IppException("'$tag' is not a valid group tag")
    }

    fun put(attribute: IppAttribute<*>): IppAttribute<*>? {
        attribute.checkSyntax()
        if (attribute.values.isEmpty() && !attribute.tag.isOutOfBandTag()) {
            throw IllegalArgumentException("empty value list")
        }
        val replaced = put(attribute.name, attribute)
        if (replaced != null) {
            log.warn { "replaced '$replaced' with '$attribute' in group $tag" }
        }
        return replaced
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

    override fun toString() =
            "'$tag' ${size.toPluralString("attribute")}"

    fun logDetails(prefix: String = "", title: String = "$tag") {
        log.info { "${prefix}$title" }
        for (key in keys) {
            log.info { "$prefix  ${get(key)}" }
        }
    }

}