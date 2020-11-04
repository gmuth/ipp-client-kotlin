package de.gmuth.ext

import java.util.*

/**
 * Copyright (c) 2020 Gerhard Muth
 */

fun Calendar.dstSavingsOffset() = with(timeZone) {
    rawOffset + if (useDaylightTime() && inDaylightTime(time)) dstSavings else 0
}