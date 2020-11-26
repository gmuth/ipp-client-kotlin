package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

// RFC 8011 5.1.16.
data class IppResolution(val x: Int, val y: Int, val unit: Int) {

    constructor(x: Int, y: Int, unit: Unit = Unit.DPI) : this(x, y, unit.code)

    constructor(xy: Int, unit: Unit = Unit.DPI) : this(xy, xy, unit.code)

    override fun toString() = "${x}${if (x == y) "" else "x${y}"}${Unit.fromInt(unit)}"

    enum class Unit(val code: Int) {
        DPI(3), DPC(4);

        override fun toString() = name.toLowerCase()

        companion object {
            fun fromInt(code: Int) = values().single { it.code == code }
        }
    }

}