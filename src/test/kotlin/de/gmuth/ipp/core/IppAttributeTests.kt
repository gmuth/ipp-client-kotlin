package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppTag.*
import de.gmuth.log.Logging
import java.io.File
import java.util.logging.Logger.getLogger
import kotlin.test.*

class IppAttributeTests {

    init {
        Logging.configure()
    }

    private val logger = getLogger(javaClass.name)
    private val attribute = IppAttribute("printer-state-reasons", Keyword, "none")

    @Test
    fun constructorFailsDueToDelimiterTag() {
        assertFailsWith<IllegalArgumentException> { IppAttribute<Unit>("some-attribute-name", Operation) }
    }

    @Test
    fun constructorFailsDueToIllegalValueClass() {
        assertFailsWith<IppException> { IppAttribute("some-attribute-name", TextWithLanguage, 1) }
    }

    @Test
    fun accessingSetAsValueFails() {
        assertFailsWith<IppException> { attribute.value }
    }

    @Test
    fun additionalValue() {
        attribute.additionalValue(IppAttribute("", Keyword, "media-empty"))
        assertEquals(2, attribute.values.size)
    }

    @Test
    fun additionalValueIgnore1() {
        attribute.additionalValue(IppAttribute("", Integer, 2.1))
    }

    @Test
    fun additionalValueFails2() {
        assertFailsWith<IppException> { attribute.additionalValue(IppAttribute<Unit>("", Keyword)) }
    }

    @Test
    fun additionalValueFails3() {
        assertFailsWith<IppException> { attribute.additionalValue(IppAttribute("invalid-name", Keyword, "wtf")) }
    }

    @Test
    fun additionalValueIgnore2() {
        IppResponse().run {
            read(File("src/test/resources/invalidBrotherMediaTypeSupported.response"))
            assertEquals(1, printerGroup.getValues<List<String>>("media-type-supported").size)
        }
    }

    @Test
    fun buildAttribute() {
        assertEquals(attribute, attribute.buildIppAttribute(IppAttributesGroup(Printer)))
    }

    @Test
    fun toStringNoValue() {
        attribute.values.clear()
        assertEquals("no-values", attribute.valuesToString())
    }

    @Test
    fun toStringByteArrayNonEmpty() {
        val byteArrayAttribute = IppAttribute("", NoValue, ByteArray(2))
        assertTrue(byteArrayAttribute.toString().endsWith("2 bytes"))
    }

    @Test
    fun toStringIntRange() {
        assertTrue(IppAttribute("int-range", RangeOfInteger, 1..2).toString().endsWith("1-2"))
    }

    @Test
    fun toStringTest() {
        assertEquals("christmas-time (integer) = 1608160102 (2020-12-16T23:08:22Z)", IppAttribute("christmas-time", Integer, 1608160102).toString())
    }

    @Test
    fun enumNameOrValue() {
        assertEquals("processing", IppAttribute("printer-state", IppTag.Enum, 0).enumNameOrValue(4))
    }

    @Test
    fun log() {
        // cover an output with more than 160 characters and a collection value
        IppAttribute("media-col".padEnd(160, '-'), BegCollection, IppCollection()).log(logger)
    }

    @Test
    fun isCollection() {
        assertTrue(IppAttribute("some-collection", BegCollection, IppCollection()).isCollection())
    }

    @Test
    fun isNotCollection() {
        assertFalse(IppAttribute("some-integer", Integer, 0).isCollection())
    }

    @Test
    fun attributeToString() {
        assertEquals("foo (1setOf integer) = 1,2,3", IppAttribute("foo", Integer, 1, 2, 3).toString())
    }

    @Test
    fun attributeWithDateTimeHasValidValueClass() {
        IppAttribute("datetime-now", DateTime, IppDateTime.now()).run {
            assertTrue(tag.valueHasValidClass(value))
        }
    }

    @Test
    fun keywirdStringValues() {
        assertEquals(listOf("none"), attribute.getStringValues())
    }

    @Test
    fun nameStringValue() {
        IppAttribute("name", NameWithoutLanguage, IppString("mike")).run {
            assertEquals("mike", getStringValue())
        }
    }

    @Test
    fun exceptionOnStringValue() {
        assertFailsWith<IllegalArgumentException> {
            IppAttribute.getStringValue(0)
        }
    }

}