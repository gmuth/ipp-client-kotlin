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
    constructor(xy: Int, unit: Unit) : this(xy, xy, unit.code)

    override fun toString() = "${x}${if (x == y) "" else "x${y}"}${unitString()}"

    private fun unitString() = when (unit) {
        3 -> "dpi"
        4 -> "dpcm"
        else -> "unit-$unit"
    }

    enum class Unit(val code: Int) {
        DPI(3),
        DPCM(4)
    }

}