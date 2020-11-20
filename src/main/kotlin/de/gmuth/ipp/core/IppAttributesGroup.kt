package de.gmuth.ipp.core

import de.gmuth.ext.toPluralString
import java.nio.charset.Charset

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class IppAttributesGroup(val tag: IppTag) : LinkedHashMap<String, IppAttribute<*>>() {

    init {
        if (!tag.isDelimiterTag() || tag.isEndTag()) {
            throw IppException("'$tag' is not a valid group tag")
        }
    }

    fun put(attribute: IppAttribute<*>): IppAttribute<*>? {
        try {
            attribute.checkSyntax()
            if (!attribute.tag.isOutOfBandTag() && attribute.values.isEmpty()) {
                throw IppException("value list is empty")
            }
            val replaced = put(attribute.name, attribute)
            if (replaced != null) {
                println("WARN: replaced '$replaced' with '$attribute'")
            }
            return replaced
        } catch (exception: Exception) {
            throw IppException("failed to put attribute '${attribute.name}' to group '$tag'", exception)
        }
    }

    fun attribute(name: String, tag: IppTag, values: List<Any>) =
            put(IppAttribute(name, tag, values.toMutableList()))

    fun attribute(name: String, tag: IppTag, vararg values: Any) =
            put(IppAttribute(name, tag, values.toMutableList()))

    fun attribute(name: String, vararg values: Any) =
            put(IppAttribute(name, values.toMutableList()))

    @Suppress("UNCHECKED_CAST")
    fun <T> getValue(name: String) = get(name)?.value as T ?: throw IppException("attribute '$name' not found (in group $tag)")

    @Suppress("UNCHECKED_CAST")
    fun <T> getValues(name: String) = get(name)?.values as T ?: throw IppException("attribute '$name' not found (in group $tag)")

    fun available(name: String) = keys.contains(name)

    override fun toString() = "'$tag' with ${size.toPluralString("attribute")}"

    val attributesCharset: Charset
        get() = getValue("attributes-charset") ?: throw IppException("missing 'attributes-charset'")

    fun logDetails(prefix: String = "", title: String = "$tag") {
        println("${prefix}$title")
        for (key in keys) {
            println("$prefix  ${get(key)}")
        }
    }

}