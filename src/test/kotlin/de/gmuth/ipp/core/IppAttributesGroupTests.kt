package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppTag.*
import de.gmuth.log.Logging
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class IppAttributesGroupTests {

    private val group = IppAttributesGroup(Operation)

    @Test
    fun constructorFails1() {
        assertFailsWith<IppException> { IppAttributesGroup(End) }
    }

    @Test
    fun constructorFails2() {
        assertFailsWith<IppException> { IppAttributesGroup(Integer) }
    }

    @Test
    fun putWithReplacementAllowed() {
        with(IppAttributesGroup(Printer)) {
            attribute("number", Integer, 0)
            attribute("number", Integer, 1, 2)
            assertEquals(1, size)
            assertEquals(get("number")!!.values.size, 2)
        }
    }

    @Test
    fun putWithReplacementWarning() {
        IppAttributesGroup.log.logLevel = Logging.LogLevel.INFO
        group.put(IppAttribute("number", Integer, 0))
        group.put(IppAttribute("number", Integer, 1, 2), true)
        assertEquals(1, group.size)
        assertEquals(group.get("number")!!.values.size, 2)
    }

    @Test
    fun putEmptyValues() {
        group.attribute("empty", Integer, listOf())
        assertEquals(1, group.size)
    }

    @Test
    fun toStringValue() {
        assertEquals("'operation-attributes-tag' 0 attributes", group.toString())
    }

    @Test
    fun getValue() {
        group.attribute("foo", Keyword, "bar")
        assertEquals("bar", group.getValue("foo") as String)
        assertEquals("bar", group.getValueOrNull("foo") ?: throw NullPointerException())
        assertEquals(null, group.getValueOrNull<String>("invalid-name"))
    }

    @Test
    fun getValues() {
        group.attribute("multiple", Integer, 1, 2)
        assertEquals(listOf(1, 2), group.getValues("multiple"))
    }

    @Test
    fun getValueFails() {
        assertFailsWith<IppException> { group.getValue("invalid-name") }
    }

    @Test
    fun getValuesFails() {
        assertFailsWith<IppException> { group.getValues("invalid-name") }
    }

    @Test
    fun logDetails() {
        group.attribute("Commodore PET", Integer, 2001)
        group.logDetails("|", "title")
    }

    @Test
    fun saveAttributes() {
        group.attribute("Commodore C", Integer, 64)
        group.saveText(File.createTempFile("tempfiles", ".tmp"))
    }

    // ------------- interface Map methods ------------

    @Test
    fun mapInterfaceMethods() {
        group.remove("a")

        group.attribute("b", Unknown)
        assertTrue(group.containsKey("b"))

        val c = IppAttribute<Unit>("c", Unknown)
        group.put(c)
        assertTrue(group.containsValue(c))

        group.getOrDefault("d", IppAttribute<Unit>("default", Unknown))

        group.remove("b")
        group.remove("c", c)

        assertTrue(group.entries.size == 0)
    }

}