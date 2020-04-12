package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

// RFC8010 3.1.6.
class IppCollection : ArrayList<IppAttribute<*>>() {

    override fun toString() = joinToString(" ", "{", "}") { "${it.name}=${it.value}" }

}