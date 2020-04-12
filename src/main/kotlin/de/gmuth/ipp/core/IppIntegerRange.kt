package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

data class IppIntegerRange(
        val start: Int,
        val end: Int
) {
    override fun toString() = "$start-$end"
}