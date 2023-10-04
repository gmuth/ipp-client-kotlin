package de.gmuth.ipp.client

/**
 * Copyright (c) 2021 Gerhard Muth
 */

import de.gmuth.ipp.attributes.MediaCollection
import de.gmuth.ipp.attributes.MediaMargin
import de.gmuth.ipp.attributes.MediaSource
import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppResponse
import de.gmuth.ipp.core.IppTag
import org.junit.Test
import java.io.File

class IppMediaTests {

    @Test
    fun defaultConstructor() {
        MediaCollection().buildIppAttribute(IppAttributesGroup(IppTag.Printer))
    }

    @Test
    fun margin() {
        MediaMargin()
        MediaMargin(0)
    }

    @Test
    fun sourceNotSupported() {
        val attributes = IppResponse().apply {
            read(File("printers/Simulated_Laser_Printer/Get-Printer-Attributes.ipp"))
        }.printerGroup
        MediaCollection(source = MediaSource("invalid")).buildIppAttribute(attributes)
    }

    @Test
    fun notProvided() {
        MediaCollection(source = MediaSource("main")).buildIppAttribute(IppAttributesGroup(IppTag.Printer))
    }

}