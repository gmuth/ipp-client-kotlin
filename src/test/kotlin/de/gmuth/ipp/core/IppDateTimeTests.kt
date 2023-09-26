package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class IppDateTimeTests {

    private val javaDateUtc = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").parse("2020-12-13T10:22:33.400+00:00")
    private val ippDateTime3HoursEast = IppDateTime(2020, 12, 13, 13, 22, 33, 4, '+', 3, 0)
    private val ippDateTime1HourWest = IppDateTime(2020, 12, 13, 9, 22, 33, 4, '-', 1, 0)

    @Test
    fun toRFC2579() {
        assertEquals("2020-12-13,13:22:33.4,+3:0", ippDateTime3HoursEast.toRFC2579())
    }

    @Test
    fun toString_ISO8601() {
        assertEquals("2020-12-13T13:22:33.4+03:00", ippDateTime3HoursEast.toString())
    }

    @Test
    fun toZonedDateTimeEast() {
        assertEquals("2020-12-13T13:22:33.400+03:00", ippDateTime3HoursEast.toZonedDateTime().toString())
    }

    @Test
    fun toZonedDateTimeWest() {
        assertEquals("2020-12-13T09:22:33.400-01:00", ippDateTime1HourWest.toZonedDateTime().toString())
    }

    @Test
    fun zonedDateTimeConstructorEast() {
        assertEquals(ippDateTime3HoursEast, IppDateTime(ZonedDateTime.parse("2020-12-13T13:22:33.400+03:00")))
    }

    @Test
    fun zonedDateTimeConstructorWest() {
        assertEquals(ippDateTime1HourWest, IppDateTime(ZonedDateTime.parse("2020-12-13T09:22:33.400-01:00")))
    }

    @Test
    fun toCalendarEast() {
        ippDateTime3HoursEast.toCalendar().run {
            assertEquals(2020, get(Calendar.YEAR))
            assertEquals(12 - 1, get(Calendar.MONTH))
            assertEquals(13, get(Calendar.DAY_OF_MONTH))
            assertEquals(13, get(Calendar.HOUR_OF_DAY))
            assertEquals(22, get(Calendar.MINUTE))
            assertEquals(33, get(Calendar.SECOND))
            assertEquals(400, get(Calendar.MILLISECOND))
            assertEquals("GMT+03:00", timeZone.id)
        }
    }

    @Test
    fun toCalendarWest() {
        ippDateTime1HourWest.toCalendar().run {
            assertEquals(2020, get(Calendar.YEAR))
            assertEquals(12 - 1, get(Calendar.MONTH))
            assertEquals(13, get(Calendar.DAY_OF_MONTH))
            assertEquals(9, get(Calendar.HOUR_OF_DAY))
            assertEquals(22, get(Calendar.MINUTE))
            assertEquals(33, get(Calendar.SECOND))
            assertEquals(400, get(Calendar.MILLISECOND))
            assertEquals("GMT-01:00", timeZone.id)
        }
    }

    @Test
    fun calendarWestConstructor() {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT-01:00")).apply {
            set(Calendar.YEAR, 2020)
            set(Calendar.MONTH, 12 - 1)
            set(Calendar.DAY_OF_MONTH, 13)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 22)
            set(Calendar.SECOND, 33)
            set(Calendar.MILLISECOND, 400)
        }
        assertEquals(ippDateTime1HourWest, IppDateTime(calendar))
    }

    @Test
    fun calendarConstructorWithDaylightSaving() {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin")).apply {
            set(Calendar.YEAR, 2020)
            set(Calendar.MONTH, 6 - 1)
            set(Calendar.DAY_OF_MONTH, 13)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 22)
            set(Calendar.SECOND, 33)
            set(Calendar.MILLISECOND, 400)
        }
        assertEquals(12, IppDateTime(calendar).hour)
    }

    @Test
    fun calendarConstructorWithoutDaylightSaving() {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin")).apply {
            set(Calendar.YEAR, 2020)
            set(Calendar.MONTH, 12 - 1)
            set(Calendar.DAY_OF_MONTH, 13)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 22)
            set(Calendar.SECOND, 33)
            set(Calendar.MILLISECOND, 400)
        }
        assertEquals(12, IppDateTime(calendar).hour)
    }

    @Test
    fun toDate() {
        GregorianCalendar(TimeZone.getTimeZone("UTC")).run {
            time = ippDateTime3HoursEast.toDate()
            assertEquals(2020, get(Calendar.YEAR))
            assertEquals(12 - 1, get(Calendar.MONTH))
            assertEquals(13, get(Calendar.DAY_OF_MONTH))
            assertEquals(10, get(Calendar.HOUR_OF_DAY))
            assertEquals(22, get(Calendar.MINUTE))
            assertEquals(33, get(Calendar.SECOND))
            assertEquals(400, get(Calendar.MILLISECOND))
            assertEquals("UTC", timeZone.id)
        }
    }

    @Test
    fun dateConstructor() {
        IppDateTime(javaDateUtc).run {
            assertEquals(2020, year)
            assertEquals(12, month)
            assertEquals(13, day)
            assertEquals(10, hour)
            assertEquals(22, minutes)
            assertEquals(33, seconds)
            assertEquals(4, deciSeconds)
        }
    }

}