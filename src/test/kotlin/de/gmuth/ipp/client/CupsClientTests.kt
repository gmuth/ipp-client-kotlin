package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2025 Gerhard Muth
 */

import de.gmuth.ipp.attributes.PrinterState
import de.gmuth.ipp.core.IppException
import de.gmuth.ipp.core.IppStatus.ClientErrorNotFound
import org.junit.Test
import java.net.URI
import java.util.logging.Logger.getLogger
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CupsClientTests {
    private val logger = getLogger(javaClass.name)
    val ippClientMock = IppClientMock("printers/CUPS")
    val cupsClient = CupsClient(URI.create("ipps://cups"), ippClient = ippClientMock)

    @Test
    fun constructors() {
        CupsClient()
        CupsClient(URI("ipps://host"))
    }

    @Test
    fun getPrinters() {
        ippClientMock.mockResponse("Cups-Get-Printers.ipp")
        cupsClient.getPrinters().run {
            forEach { logger.info { it.toString() } }
            assertEquals(12, size)
        }
    }

    @Test
    fun getPrinter() {
        ippClientMock.mockResponse("Get-Printer-Attributes.ipp", "printers/CUPS_HP_LaserJet_100_color_MFP_M175")
        cupsClient.getPrinter("ColorJet_HP").run {
            log(logger)
            assertEquals("HP LaserJet 100 color MFP M175", makeAndModel.text)
            assertEquals(PrinterState.Idle, state)
            assertEquals(5, markers.size)
            assertTrue(isAcceptingJobs)
            assertTrue(isCups())
            assertEquals("2.2.5", cupsVersion)
        }
    }

    @Test
    fun getCupsVersion() {
        ippClientMock.mockResponse("Get-Printer-Attributes.ipp", "printers/CUPS_HP_LaserJet_100_color_MFP_M175")
        assertEquals("2.2.5", cupsClient.version)
    }

    @Test
    fun getPrinterFails() {
        assertFailsWith<IppException> { // no such cups printer
            cupsClient.getPrinter("invalid")
        }
    }

    @Test
    fun getDefault() {
        ippClientMock.mockResponse("Cups-Get-Default.ipp")
        cupsClient.getDefault().run {
            assertEquals("ColorJet_HP", name.text)
        }
    }

    @Test
    fun getDefaultFails() {
        val exception = assertFailsWith<IppOperationException> {
            ippClientMock.mockResponse("Cups-Get-Default-Error.ipp")
            cupsClient.getDefault()
        }
        assertTrue(exception.statusIs(ClientErrorNotFound))
    }

    @Test
    fun setDefault() {
        cupsClient.setDefault("matrix")
    }

}