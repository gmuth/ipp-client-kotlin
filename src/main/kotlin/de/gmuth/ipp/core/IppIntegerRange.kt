package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

// Kotlin IntRange not used in favour of portability and overriding toString()

class IppIntegerRange(val start: Int, val end: Int) {

    override fun toString() = "$start-$end"

}