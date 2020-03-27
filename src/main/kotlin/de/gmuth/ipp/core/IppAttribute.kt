package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class IppAttribute<T>(
        val name: String,
        val tag: IppTag,
        val values: List<T>
) {
    constructor(name: String, tag: IppTag, vararg values: T) : this(name, tag, values.toList())
    //constructor(name: String, vararg values: T) : this(name, lookupIppTag(name), values.toList())

    fun isSetOf() = values.size > 1

    val value: T
        get() = if (values.size == 1) values.first() else
            throw IllegalStateException("found ${values.size.toPluralString("value")} but expected only 1 for '$name'")

    override fun toString() = String.format("%s (%s) = %s", name, if (isSetOf()) "1setOf $tag" else "$tag", values.joinToString(","))

}