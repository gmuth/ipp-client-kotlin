package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

// RFC2579
data class IppDateTime(
        val year: Int,
        val month: Int,
        val day: Int,
        val hour: Int,
        val minutes: Int,
        val seconds: Int,
        val deciSeconds: Int,
        val directionFromUTC: Char, // '+', '-'
        val hoursFromUTC: Int,
        val minutesFromUTC: Int
) {

    override fun toString() = toZonedDateTime().toString()

    fun toRFC2579() = format("%d-%d-%d,%d:%d:%d.%d,%c%d:%d")

    fun toISO8601() = format("%04d-%02d-%02dT%02d:%02d:%02d.%01d%c%02d:%02d")

    private fun format(format: String) = String.format(
            format, year, month, day, hour, minutes, seconds, deciSeconds, directionFromUTC, hoursFromUTC, minutesFromUTC
    )

    fun toZonedDateTime(): ZonedDateTime = ZonedDateTime.of(
            LocalDateTime.of(year, month, day, hour, minutes, seconds, deciSeconds * 100 * 1000 * 1000),
            ZoneOffset.ofTotalSeconds((if (directionFromUTC == '-') -1 else 1) * (hoursFromUTC * 60 + minutesFromUTC) * 60)
    )

    companion object {
        fun fromZonedDateTime(zonedDateTime: ZonedDateTime): IppDateTime {
            with(zonedDateTime) {
                val offsetTotalMinutes = zone.rules.getOffset(zonedDateTime.toLocalDateTime()).totalSeconds / 60
                return IppDateTime(
                        year,
                        monthValue,
                        dayOfMonth,
                        hour,
                        minute,
                        second,
                        get(ChronoField.MILLI_OF_SECOND) / 100,
                        if (offsetTotalMinutes < 0) '-' else '+',
                        offsetTotalMinutes.absoluteValue / 60,
                        offsetTotalMinutes.absoluteValue % 60
                )
            }
        }

        fun now() = fromZonedDateTime(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS))
    }
}