package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

// RFC8010 3.1.6.
class IppCollection() {

    // with inheritance the attribute value would be handled as "normal list"
    val members = mutableListOf<IppAttribute<*>>()

    constructor(vararg attributes: IppAttribute<*>) : this(attributes.toList())

    constructor(attributes: Collection<IppAttribute<*>>) : this() {
        members.addAll(attributes)
    }

    fun add(attribute: IppAttribute<*>) = members.add(attribute)

    override fun toString() = members.joinToString(" ", "{", "}") { "${it.name}=${it.value}" }

}