package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class IppAttribute<T>(val name: String, val tag: IppTag, val values: List<T>) {

    constructor(name: String, tag: IppTag, vararg values: T) : this(name, tag, values.toList())

    constructor(name: String, vararg values: T) : this(name, IppRegistrations.tagForAttribute(name), values.toList()) {
        if (!supportTagForAttribute) throw RuntimeException("for '$name' use IppTag.${tag.name}")
    }

    fun is1setOf() = values.size > 1
            || IppRegistrations.attributeNameIsRegistered(name) && IppRegistrations.attributeIs1setOf(name)

    val value: T
        get() =
            if (values.size == 1) values.first()
            else throw IppException("found ${values.size.toPluralString("value")} but expected only 1 for '$name'")

    override fun toString(): String {
        return String.format("%s (%s) = %s", name, if (is1setOf()) "1setOf $tag" else "$tag", values.joinToString(","))
    }

    companion object {
        var supportTagForAttribute: Boolean = true
    }

}