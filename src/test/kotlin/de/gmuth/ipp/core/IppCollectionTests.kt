package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import kotlin.test.Test
import kotlin.test.assertEquals

class IppCollectionTests {

    private val collection = IppCollection(IppAttribute("foo", IppTag.Keyword, "a", "b"))

    @Test
    fun toStringValue() {
        assertEquals("{foo=a,b}", collection.toString())
    }

    @Test
    fun attribute() {
        collection.attribute("year", IppTag.Integer, 2021)
        assertEquals(2, collection.members.size)
    }

    @Test
    fun logDetailsNarrow() {
        collection.logDetails()
    }

    @Test
    fun logDetailsWide() {
        collection.addAll(listOf(IppAttribute("bar", IppTag.Keyword, "c".repeat(160))))
        collection.logDetails()
    }

}