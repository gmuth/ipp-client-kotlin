package de.gmuth.ipp.client

/**
 * Copyright (c) 2021-2024 Gerhard Muth
 */

import de.gmuth.ipp.attributes.MediaCollection
import de.gmuth.ipp.attributes.MediaMargin
import de.gmuth.ipp.attributes.MediaSize
import de.gmuth.ipp.attributes.MediaSource
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppCollection
import de.gmuth.ipp.core.IppResponse
import de.gmuth.ipp.core.IppTag
import de.gmuth.log.Logging
import org.junit.Test
import java.io.File
import java.util.logging.Logger
import kotlin.test.assertEquals

class IppMediaTests {

    val logger = Logger.getLogger(javaClass.name)

    init {
        Logging.configure()
    }

    val emptyPrinterAttributes = IppAttributesGroup(IppTag.Printer)
    val xeroxB210Attributes = IppResponse()
        .apply { read(File("printers/Xerox_B210_Printer/001-Get-Printer-Attributes.res")) }
        .printerGroup

    @Test
    fun defaultConstructor() {
        MediaCollection().buildIppAttribute(emptyPrinterAttributes)
    }

    @Test
    fun margin() {
        MediaMargin()
        MediaMargin(0)
    }

    @Test
    fun sourceNotSupported() {
        MediaCollection(source = MediaSource("invalid")).buildIppAttribute(xeroxB210Attributes)
    }

    @Test
    fun notProvided() {
        MediaCollection(source = MediaSource("main")).buildIppAttribute(emptyPrinterAttributes)
    }

    @Test
    fun mediaColDefault() {
        val ippCollection = xeroxB210Attributes.getValue<IppCollection>("media-col-default")
        with(MediaCollection.fromIppCollection(ippCollection)) {
            // media-size={x-dimension=21000 y-dimension=29700}
            assertEquals(MediaSize.ISO_A4, size)
            // media-type=stationery
            assertEquals("stationery", type)
            // media-source=tray-1
            assertEquals(MediaSource.Tray1, source)
            // media-top-margin=440 media-bottom-margin=440 media-left-margin=440 media-right-margin=440
            assertEquals(MediaMargin(440), margin) // uses convenience short cut for same margins
        }
    }

    //@Test
    fun listXeroxMediaAttributes() {
        xeroxB210Attributes
            .values
            .filter { it.name.contains("media") }
            .forEach { logger.info { "$it" } }
    }

}