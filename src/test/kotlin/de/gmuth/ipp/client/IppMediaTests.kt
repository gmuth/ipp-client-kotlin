package de.gmuth.ipp.client

import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppResponse
import de.gmuth.ipp.core.IppTag
import org.junit.Test
import java.io.File

class IppMediaTests {

    @Test
    fun defaultConstructor() {
        IppMedia.Collection().buildIppAttribute(IppAttributesGroup(IppTag.Printer))
    }

    @Test
    fun margins() {
        IppMedia.Margins()
        IppMedia.Margins(0)
    }

    @Test
    fun sourceNotSupported() {
        val attributes = IppResponse().apply {
            read(File("printers/Simulated_Laser_Printer/Get-Printer-Attributes.ipp"))
        }.printerGroup
        IppMedia.Collection(source = "invalid").buildIppAttribute(attributes)
    }

    @Test
    fun notProvided() {
        IppMedia.Collection(source = "main").buildIppAttribute(IppAttributesGroup(IppTag.Printer))
    }

}