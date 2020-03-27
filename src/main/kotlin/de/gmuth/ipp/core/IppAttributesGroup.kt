package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class IppAttributesGroup(val tag: IppTag) : LinkedHashMap<String, IppAttribute<*>>() {

    init {
        if (!tag.isGroupTag()) throw IllegalArgumentException("'$tag' is not a group tag")
    }

    fun put(newAttribute: IppAttribute<*>) {
        //with(newAttribute) { println("** put $name = $value --- ${value?.javaClass}") }
        val oldAttribute = put(newAttribute.name, newAttribute)
        if (oldAttribute != null) {
            println(String.format("replaced '%s' with '%s'", oldAttribute, newAttribute))
        }
    }

    fun put(name: String, tag: IppTag, value: Any) {
        put(when (value) {
            is String -> IppAttribute(name, tag, value)
            else -> throw IllegalArgumentException()
        })
    }

    //fun put(name: String, value: Any) = put(name, IppAttribute.lookupIppTag(name), value)

    override fun toString(): String {
        return String.format("IppAttributesGroup '%s' containing %d attributes", tag, size)
    }

    fun logDetails(prefix: String) {
        println("${prefix}$tag group")
        for (key in keys) println("${prefix}  ${get(key)}")
    }

}