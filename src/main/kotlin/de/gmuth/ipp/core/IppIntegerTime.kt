package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

data class IppIntegerTime(val seconds: Int) {

    fun toLocalDateTime() = LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds.toLong()), ZoneId.systemDefault())

    override fun toString() = "$seconds (${toLocalDateTime()})"

    companion object {
        fun fromInt(seconds: Int? = null): IppIntegerTime? = if (seconds == null) null else IppIntegerTime(seconds)

        fun fromLocalDateTime(localDateTime: LocalDateTime) = IppIntegerTime(
                localDateTime.toEpochSecond(ZoneId.systemDefault().rules.getOffset(localDateTime)).toInt()
        )

        fun now() = fromLocalDateTime(LocalDateTime.now())
    }

}