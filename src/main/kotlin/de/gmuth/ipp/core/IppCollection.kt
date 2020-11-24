package de.gmuth.ipp.core

import de.gmuth.log.Log

/**
 * Copyright (c) 2020 Gerhard Muth
 */

// RFC8010 3.1.6.
class IppCollection() {

    companion object {
        val log = Log.getWriter("IppCollection", Log.Level.INFO)
    }

    // with inheritance the attribute value would be handled as "normal list"
    val members = mutableListOf<IppAttribute<*>>()

    constructor(vararg attributes: IppAttribute<*>) : this(attributes.toList())

    constructor(attributes: Collection<IppAttribute<*>>) : this() {
        members.addAll(attributes)
    }

    fun add(attribute: IppAttribute<*>) = members.add(attribute)

    fun addAll(attributes: Collection<IppAttribute<*>>) = members.addAll(attributes)

    override fun toString() = members.joinToString(" ", "{", "}") {
        "${it.name}=${it.values.joinToString(",")}"
    }

    fun logDetails(prefix: String = "") {
        val string = toString()
        if (string.length < 160) {
            log.info { "$prefix$string" }
        } else {
            for (member in members) {
                member.logDetails("$prefix")
            }
        }
    }

}