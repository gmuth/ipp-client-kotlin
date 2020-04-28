package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class IppAttributesGroup(val tag: IppTag) : LinkedHashMap<String, IppAttribute<*>>() {

    init {
        if (!tag.isDelimiterTag()) {
            throw IppException("'$tag' is not a delimiter tag")
        }
    }

    fun put(attribute: IppAttribute<*>): IppAttribute<*>? {
        if (!attribute.tag.isOutOfBandTag() && attribute.values.isEmpty()) {
            throw IppException("put attribute rejected: value list is empty")
        }
        val replaced = put(attribute.name, attribute)
        if (replaced != null) {
            println("WARN: replaced '$replaced' with '$attribute'")
        }
        return replaced
    }

    private fun attribute(name: String, tag: IppTag, values: List<*>) =
            put(IppAttribute(name, tag, values.toMutableList()))

    fun attribute(name: String, tag: IppTag, vararg values: Any?) =
            put(IppAttribute(name, tag, values.toMutableList()))

    fun attribute(name: String, vararg values: Any?) =
            put(IppAttribute(name, values.toMutableList()))

    @Suppress("UNCHECKED_CAST")
    fun <T> getValue(name: String) = get(name)?.value as T

    @Suppress("UNCHECKED_CAST")
    fun <T> getValues(name: String) = get(name)?.values as T

    fun checkValueSupported(name: String, value: Any) {
        if (tag != IppTag.Printer) {
            throw IppException("expected printer attributes group")
        }
        with(get(name)) {
            if (this != null) {
                if (!values.contains(value)) {
                    throw IppException("'$value' not supported by printer. $this")
                } else {
                    //println("'$value' is supported by printer. $this")
                }
            }
        }
    }

    override fun toString() = "IppAttributesGroup '$tag' containing ${size.toPluralString("attribute")}"

    fun logDetails(prefix: String = "") {
        println("${prefix}$tag")
        for (key in keys) {
            println("$prefix  ${get(key)}")
        }
    }

}