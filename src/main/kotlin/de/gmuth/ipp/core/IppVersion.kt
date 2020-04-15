package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

data class IppVersion(
        val major: Int,
        val minor: Int
) {
    override fun toString() = "$major.$minor"
}