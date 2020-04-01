package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class IppAttributesGroup(val tag: IppTag) : LinkedHashMap<String, IppAttribute<*>>() {

    init {
        if (!tag.isGroupTag()) throw IppException("'$tag' is not a group tag")
    }

    fun put(attribute: IppAttribute<*>): IppAttribute<*>? {
        if(attribute.value == null) return null
        val replaced = put(attribute.name, attribute)
        if (replaced != null) {
            println(String.format("replaced '%s' with '%s'", replaced, attribute))
        }
        return replaced
    }

    fun attribute(name: String, tag: IppTag, value: Any?) = put(IppAttribute(name, tag, value))

    fun attribute(name: String, value: Any?) = put(IppAttribute(name, value))

    override fun toString(): String {
        return "IppAttributesGroup '$tag' containing ${size.toPluralString("attribute")}"
    }

    fun logDetails(prefix: String) {
        if (size > 0) {
            println("${prefix}$tag")
            for (key in keys) println("${prefix}  ${get(key)}")
        }
    }

}