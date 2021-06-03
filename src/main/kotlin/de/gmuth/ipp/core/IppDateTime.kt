package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoField
import java.util.*
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
        val directionFromUTC: Char, // '+' or '-'
        val hoursFromUTC: Int,
        val minutesFromUTC: Int
) {

    override fun toString() = toISO8601()

    fun toRFC2579() = format("%d-%d-%d,%d:%d:%d.%d,%c%d:%d")

    fun toISO8601() = format("%04d-%02d-%02dT%02d:%02d:%02d.%01d%c%02d:%02d")

    private fun format(format: String) =
            String.format(format, year, month, day, hour, minutes, seconds, deciSeconds, directionFromUTC, hoursFromUTC, minutesFromUTC)

    // support for java.time.ZonedDateTime

    constructor(zonedDateTime: ZonedDateTime) : this(
            zonedDateTime.year,
            zonedDateTime.monthValue,
            zonedDateTime.dayOfMonth,
            zonedDateTime.hour,
            zonedDateTime.minute,
            zonedDateTime.second,
            deciSeconds = zonedDateTime.get(ChronoField.MILLI_OF_SECOND) / 100,
            offsetMinutes = zonedDateTime.zone.rules.getOffset(zonedDateTime.toLocalDateTime()).totalSeconds / 60
    )

    fun toZonedDateTime(): ZonedDateTime =
            ZonedDateTime.of(
                    LocalDateTime.of(
                            year,
                            month,
                            day,
                            hour,
                            minutes,
                            seconds,
                            deciSeconds * 100 * 1000 * 1000 // nanoSeconds
                    ),
                    ZoneOffset.ofTotalSeconds(getOffsetMinutes() * 60)
            )

    // support for java.util.Calendar

    constructor(calendar: Calendar) : this(
            calendar[Calendar.YEAR],
            calendar[Calendar.MONTH] + 1,
            calendar[Calendar.DAY_OF_MONTH],
            calendar[Calendar.HOUR_OF_DAY],
            calendar[Calendar.MINUTE],
            calendar[Calendar.SECOND],
            deciSeconds = calendar[Calendar.MILLISECOND] / 100,
            offsetMinutes = calendar.dstSavingsOffsetMillis() / 1000 / 60
    )

    fun toCalendar(): Calendar =
            Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minutes)
                set(Calendar.SECOND, seconds)
                set(Calendar.MILLISECOND, deciSeconds * 100)
                timeZone = TimeZone.getTimeZone(getTimeZoneId())
            }

    // support for java.util.Date

    constructor(date: Date) :
            this(Calendar.getInstance().apply {
                time = date
                timeZone = TimeZone.getTimeZone("UTC")
            })

    fun toDate(): Date =
            toCalendar().time

    // support for offset minutes with direction

    private constructor(
            year: Int,
            month: Int,
            day: Int,
            hour: Int,
            minutes: Int,
            seconds: Int,
            deciSeconds: Int,
            offsetMinutes: Int
    ) : this(
            year,
            month,
            day,
            hour,
            minutes,
            seconds,
            deciSeconds,
            directionFromUTC = if (offsetMinutes < 0) '-' else '+',
            hoursFromUTC = offsetMinutes.absoluteValue / 60,
            minutesFromUTC = offsetMinutes.absoluteValue % 60
    )

    internal fun getOffsetMinutes() =
            (if (directionFromUTC == '-') -1 else 1) * (hoursFromUTC * 60 + minutesFromUTC)

    internal fun getTimeZoneId() =
            "GMT%c%02d%02d".format(directionFromUTC, hoursFromUTC, minutesFromUTC)

}

// Calendar extension
fun Calendar.dstSavingsOffsetMillis() =
        with(timeZone) {
            rawOffset + if (useDaylightTime() && inDaylightTime(time)) dstSavings else 0
        }