package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

data class IppVersion(
        val major: Int = 1,
        val minor: Int = 1
) {
    override fun toString() = "$major.$minor"
}