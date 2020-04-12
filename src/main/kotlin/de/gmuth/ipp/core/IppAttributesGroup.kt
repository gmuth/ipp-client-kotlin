package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class IppAttributesGroup(val tag: IppTag) : LinkedHashMap<String, IppAttribute<*>>() {

    init {
        if (!tag.isGroupTag()) throw IppException("'$tag' is not a group tag")
    }

    fun put(attribute: IppAttribute<*>): IppAttribute<*>? {
        if (attribute.tag != IppTag.NoValue && attribute.values.isEmpty())
            throw IppException("missing attribute values to put")
        val replaced = put(attribute.name, attribute)
        if (replaced != null) println("replaced '$replaced' with '$attribute'")
        return replaced
    }

    fun attribute(name: String, tag: IppTag, values: List<*>) = put(IppAttribute(name, tag, values.toMutableList()))

    fun attribute(name: String, tag: IppTag, vararg value: Any?) = put(IppAttribute(name, tag, value.toMutableList()))

    fun attribute(name: String, vararg value: Any?) = put(IppAttribute(name, value.toMutableList()))

    override fun toString() = "IppAttributesGroup '$tag' containing ${size.toPluralString("attribute")}"

    fun logDetails(prefix: String) {
        println("${prefix}$tag")
        for (key in keys) println("${prefix}  ${get(key)}")
    }

}