package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.core.IppTag.*
import java.io.File
import java.util.logging.Logger.getLogger
import kotlin.test.*

class IppAttributeTests {

    private val ippAttribute = IppAttribute("printer-state-reasons", Keyword, "none")
    val tlog = getLogger(javaClass.name)

    @Test
    fun constructorFailsDueToDelimiterTag() {
        assertFailsWith<IppException> { IppAttribute<Unit>("some-attribute-name", Operation) }
    }

    @Test
    fun constructorFailsDueToIllegalValueClass() {
        assertFailsWith<IppException> { IppAttribute("some-attribute-name", TextWithLanguage, 1) }
    }

    @Test
    fun accessingSetAsValueFails() {
        assertFailsWith<IppException> { ippAttribute.value }
    }

    @Test
    fun additionalValue() {
        ippAttribute.additionalValue(IppAttribute("", Keyword, "media-empty"))
        assertEquals(2, ippAttribute.values.size)
    }

    @Test
    fun additionalValueIgnore1() {
        ippAttribute.additionalValue(IppAttribute("", Integer, 2.1))
    }

    @Test
    fun additionalValueFails2() {
        assertFailsWith<IppException> { ippAttribute.additionalValue(IppAttribute<Unit>("", Keyword)) }
    }

    @Test
    fun additionalValueFails3() {
        assertFailsWith<IppException> { ippAttribute.additionalValue(IppAttribute("invalid-name", Keyword, "wtf")) }
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
        assertEquals(ippAttribute, ippAttribute.buildIppAttribute(IppAttributesGroup(Printer)))
    }

    @Test
    fun toStringNoValue() {
        ippAttribute.values.clear()
        assertTrue(ippAttribute.toString().endsWith("no-value"))
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
    fun toStringTime1() {
        IppAttribute("some-time", Integer, 1000).toString()
    }

    @Test
    fun toStringTime2() {
        IppAttribute("christmas-time", Integer, 1608160102).toString()
    }

    @Test
    fun toStringTimeOut() {
        IppAttribute("some-time-out", Integer, 1000).toString()
    }


    @Test
    fun enumNameOrValue() {
        assertEquals("processing", IppAttribute("printer-state", IppTag.Enum, 0).enumNameOrValue(4))
    }

    @Test
    fun log() {
        // cover an output with more than 160 characters and a collection value
        IppAttribute("media-col".padEnd(160, '-'), BegCollection, IppCollection()).log(tlog)
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

}