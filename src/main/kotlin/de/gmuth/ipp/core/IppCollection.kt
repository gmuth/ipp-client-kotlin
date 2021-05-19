package de.gmuth.ipp.core

import de.gmuth.log.Logging

/**
 * Copyright (c) 2020 Gerhard Muth
 */

// RFC8010 3.1.6.
data class IppCollection(val members: MutableList<IppAttribute<*>> = mutableListOf()) {

    companion object {
        val log = Logging.getLogger {}
    }

    constructor(vararg attributes: IppAttribute<*>) : this(attributes.toMutableList())

    fun add(attribute: IppAttribute<*>) =
            members.add(attribute)

    fun addAll(attributes: Collection<IppAttribute<*>>) =
            members.addAll(attributes)

    override fun toString() =
            members.joinToString(" ", "{", "}") {
                "${it.name}=${it.values.joinToString(",")}"
            }

    fun logDetails(prefix: String = "") {
        val string = toString()
        if (string.length < 160) {
            log.info { "$prefix$string" }
        } else {
            members.forEach { it.logDetails(prefix) }
        }
    }

}