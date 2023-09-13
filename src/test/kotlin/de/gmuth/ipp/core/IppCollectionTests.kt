package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.util.NoSuchElementException
import java.util.logging.Logger.getLogger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IppCollectionTests {

    val tlog = getLogger(javaClass.name)
    private val collection = IppCollection(IppAttribute("foo", IppTag.Keyword, "a", "b"))

    @Test
    fun toStringValue() {
        assertEquals("{foo=a,b}", collection.toString())
    }

    @Test
    fun attribute() {
        collection.addAttribute("year", IppTag.Integer, 2021)
        assertEquals(2, collection.members.size)
    }

    @Test
    fun getMember() {
        val fooAttribute = collection.getMember<IppAttribute<Any>>("foo")
        assertEquals(2, fooAttribute.values.size)
    }

    @Test
    fun getMemberFails() {
        assertFailsWith<NoSuchElementException> {
            collection.getMember<Any>("does-not-exist")
        }
    }

    @Test
    fun logNarrow() {
        collection.log(tlog)
    }

    @Test
    fun logWide() {
        collection.addAll(listOf(IppAttribute("bar", IppTag.Keyword, "c".repeat(160))))
        collection.log(tlog)
    }

}