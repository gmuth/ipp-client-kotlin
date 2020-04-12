package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

// RFC8010 3.1.6.
class IppCollection {

    val members: MutableList<IppAttribute<*>> = mutableListOf()

    override fun toString() = members.joinToString(" ", "{", "}") { "${it.name}=${it.value}" }

    fun logDetails(prefix: String = "") = members.forEach { member -> println("$prefix$member") }

}