package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

data class IppResolution(
        val x: Int,
        val y: Int,
        val unit: Int
) {
    override fun toString() = "${x}${if (x == y) "" else "x${y}"}${unitValue()}"

    // where is the resolution unit specified?
    // https://github.com/apple/cups/blob/master/cups/ipp.h
    private fun unitValue() = when (unit) {
        3 -> "dpi"
        4 -> "dpcm"
        else -> "unit-$unit"
    }
}