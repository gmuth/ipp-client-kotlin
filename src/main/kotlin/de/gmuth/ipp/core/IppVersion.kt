package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class IppVersion(val major: Int = 1, val minor: Int = 1) {

    override fun toString() = "$major.$minor"

}