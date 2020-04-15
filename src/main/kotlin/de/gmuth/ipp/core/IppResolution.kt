package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

// RFC 8011 5.1.16.
data class IppResolution(
        val x: Int,
        val y: Int,
        val unit: Int
) {
    override fun toString() = "${x}${if (x == y) "" else "x${y}"}${unitString()}"

    private fun unitString() = when (unit) {
        3 -> "dpi"
        4 -> "dpcm"
        else -> "unit-$unit"
    }
}