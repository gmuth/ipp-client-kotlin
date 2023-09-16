package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2022 Gerhard Muth
 */

import de.gmuth.ipp.core.IppTag.*
import java.io.File
import java.util.logging.Logger.getLogger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class IppAttributesGroupTests {

    val log = getLogger(javaClass.name)
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
        group.put(IppAttribute("number", Integer, 0))
        group.put(IppAttribute("number", Integer, 1, 2), true)
        assertEquals(1, group.size)
        assertEquals(group["number"]!!.values.size, 2)
    }

    @Test
    fun putEmptyValues() {
        group.attribute("empty", Integer, listOf())
        assertEquals(1, group.size)
    }

    @Test
    fun putAttributesGroup() {
        val fooGroup = IppAttributesGroup(Operation).apply {
            attribute("one", Integer, 1)
            attribute("two", Integer, 2)
        }
        group.put(fooGroup)
        assertEquals(2, group.size)
    }

    @Test
    fun toStringValue() {
        assertEquals("'operation-attributes-tag' 0 attributes", group.toString())
    }

    @Test
    fun getValue() {
        group.attribute("foo", Keyword, "bar")
        assertEquals("bar", group.getValue("foo") as String)
    }

    @Test
    fun getTextValue() {
        group.attribute("foo", TextWithoutLanguage, "bar".toIppString())
        assertEquals("bar", group.getTextValue("foo"))
    }

    @Test
    fun getValueOrNull() {
        group.attribute("foo0", Keyword, "bar0")
        assertEquals("bar0", group.getValueOrNull("foo0"))
        assertEquals(null, group.getValueOrNull<String>("invalid-name"))
    }

    @Test
    fun getValues() {
        group.attribute("multiple", Integer, 1, 2)
        assertEquals(listOf(1, 2), group.getValues("multiple"))
    }

    @Test
    fun getValuesOrNull() {
        group.attribute("multiple0", Integer, 0, 1, 2)
        assertEquals(listOf(0, 1, 2), group.getValuesOrNull("multiple0"))
        assertEquals(null, group.getValuesOrNull<List<Int>>("invalid-name"))
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
    fun log() {
        group.attribute("Commodore PET", Integer, 2001)
        group.log(log, prefix = "|", title = "title")
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