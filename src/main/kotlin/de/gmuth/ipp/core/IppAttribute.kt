package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class IppAttribute<T> constructor(val name: String, val tag: IppTag, val values: MutableList<T>) {

    init {
        if (tag.isGroupTag()) throw IppException("group tag '$tag' must not be used for attributes")
    }

    constructor(name: String, tag: IppTag, vararg values: T) : this(name, tag, values.toMutableList())

    constructor(name: String, vararg values: T) : this(name, IppRegistrations.tagForAttribute(name), values.toMutableList()) {
        if (!supportTagForAttribute) throw IppException("for '$name' use IppTag.${tag.name}")
    }

    @Suppress("UNCHECKED_CAST")
    fun addValue(value: Any?) = values.add(value as T)

    fun is1setOf() = values.size > 1
            || IppRegistrations.attributeNameIsRegistered(name) && IppRegistrations.attributeIs1setOf(name)

    val value: T
        get() =
            if (values.size == 1) values.first()
            else throw IppException("found ${values.size.toPluralString("value")} but expected only 1 for '$name'")

    override fun toString(): String {
        val valuesString = if(values == null || values.isEmpty()) "" else "= " + try {
            values.joinToString(",")
        } catch (exception: Exception) {
            "<${exception.message}>"
        }
        return String.format("%s (%s) %s", name, if (is1setOf()) "1setOf $tag" else "$tag", valuesString)
    }

    companion object {
        var supportTagForAttribute: Boolean = true
    }

}