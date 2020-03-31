package de.gmuth.ipp.core

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class IppDateTime {

    companion object {

        // printer simulator used epoch seconds? but RFC 8011 says ticks since start up :-(

        fun toLocalDateTime(seconds: Int?) =
                if (seconds == null) null
                else LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds.toLong()), ZoneId.systemDefault())

        fun toInt(localDateTime: LocalDateTime) =
                if (localDateTime == null) null
                else localDateTime.toEpochSecond(ZoneOffset.MIN).toInt()

    }

}