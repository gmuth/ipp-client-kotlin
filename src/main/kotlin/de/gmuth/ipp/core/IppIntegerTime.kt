package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

data class IppIntegerTime(val seconds: Int? = null) {

    fun toLocalDateTime() =
            if (seconds == null) null
            else LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds.toLong()), ZoneId.systemDefault())

    override fun toString() = "$seconds ${if (seconds == null) "" else toLocalDateTime()?.toString()}"

    companion object {
        fun fromLocalDateTime(localDateTime: LocalDateTime) = IppIntegerTime(
                localDateTime.toEpochSecond(ZoneId.systemDefault().rules.getOffset(localDateTime)).toInt()
        )

        fun now() = fromLocalDateTime(LocalDateTime.now())
    }

}