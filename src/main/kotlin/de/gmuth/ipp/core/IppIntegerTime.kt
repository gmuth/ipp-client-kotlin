package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class IppIntegerTime(val seconds: Int? = null) {

    constructor(localDateTime: LocalDateTime) : this(
            localDateTime.toEpochSecond(ZoneId.systemDefault().rules.getOffset(localDateTime)).toInt()
    )

    fun toLocalDateTime() =
            if (seconds == null) null
            else LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds.toLong()), ZoneId.systemDefault())

    override fun toString() = "$seconds ${if (seconds == null) "" else toLocalDateTime()}"

    companion object {
        fun now() = IppIntegerTime(LocalDateTime.now())
    }
}