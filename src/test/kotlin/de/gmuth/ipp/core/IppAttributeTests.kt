package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.log.Logging
import kotlin.test.*

class IppAttributeTests {

    private val ippAttribute = IppAttribute("printer-state-reasons", IppTag.Keyword, "none")

    companion object {
        val log = Logging.getLogger(Logging.LogLevel.INFO) {}
    }

    @Test
    fun constructorFailsDueToDelimiterTag() {
        assertFailsWith<IppException> { IppAttribute<Unit>("some-attribute-name", IppTag.Operation) }
    }

    @Test
    fun constructorFailsDueToIllegalValueClass() {
        assertFailsWith<IppException> { IppAttribute("some-attribute-name", IppTag.TextWithLanguage, 1) }
    }

    @Test
    fun valueIsSet() {
        // coverage for warning
        assertEquals("none", ippAttribute.value)
    }

    @Test
    fun additionalValue() {
        ippAttribute.additionalValue(IppAttribute("", IppTag.Keyword, "media-empty"))
        assertEquals(2, ippAttribute.values.size)
    }

    @Test
    fun additionalValueFails1() {
        assertFailsWith<IppException> { ippAttribute.additionalValue(IppAttribute("", IppTag.Integer, 2.1)) }
    }

    @Test
    fun additionalValueFails2() {
        assertFailsWith<IppException> { ippAttribute.additionalValue(IppAttribute<Unit>("", IppTag.Keyword)) }
    }

    @Test
    fun additionalValueFails3() {
        assertFailsWith<IppException> { ippAttribute.additionalValue(IppAttribute("invalid-name", IppTag.Keyword, "wtf")) }
    }

    @Test
    fun buildAttribute() {
        assertEquals(ippAttribute, ippAttribute.buildIppAttribute(IppAttributesGroup(IppTag.Printer)))
    }

    @Test
    fun toStringNoValue() {
        ippAttribute.values.clear()
        assertTrue(ippAttribute.toString().endsWith("no-value"))
    }

    @Test
    fun toStringIntRange() {
        assertTrue(IppAttribute("int-range", IppTag.RangeOfInteger, 1..2).toString().endsWith("1-2"))
    }

    @Test
    fun toStringTime1() {
        IppAttribute("some-time", IppTag.Integer, 1000).toString()
    }

    @Test
    fun toStringTime2() {
        IppAttribute("christmas-time", IppTag.Integer, 1608160102).toString()
    }

    @Test
    fun toStringTimeOut() {
        IppAttribute("some-time-out", IppTag.Integer, 1000).toString()
    }


    @Test
    fun enumNameOrValue() {
        assertEquals("processing", IppAttribute("printer-state", IppTag.Enum, 0).enumNameOrValue(4))
    }

    @Test
    fun logDetails() {
        // cover an output with more than 160 characters and a collection value
        IppAttribute("media-col".padEnd(160, '-'), IppTag.BegCollection, IppCollection()).logDetails()
    }

    @Test
    fun isCollection() {
        assertTrue(IppAttribute("some-collection", IppTag.BegCollection, IppCollection()).isCollection())
    }

    @Test
    fun isNotCollection() {
        assertFalse(IppAttribute("some-integer", IppTag.Integer, 0).isCollection())
    }

}