package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class IppTime {
    companion object {

        // printer simulator uses epoch seconds? but RFC 8011 says ticks since start up :-(
        fun toLocalDateTime(seconds: Int?) =
                if (seconds == null) null
                else LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds.toLong()), ZoneId.systemDefault())

        fun toInt(localDateTime: LocalDateTime?) =
                localDateTime?.toEpochSecond(ZoneOffset.MIN)?.toInt()

    }
}