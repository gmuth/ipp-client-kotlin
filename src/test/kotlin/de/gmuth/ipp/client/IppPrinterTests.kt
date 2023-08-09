package de.gmuth.ipp.client

/**
 * Copyright (c) 2021-2022 Gerhard Muth
 */

import de.gmuth.http.HttpClientMock
import de.gmuth.io.toIppResponse
import de.gmuth.ipp.client.CupsPrinterType.Capability.CanPunchOutput
import de.gmuth.ipp.client.IppFinishing.Punch
import de.gmuth.ipp.client.IppFinishing.Staple
import de.gmuth.ipp.client.IppTemplateAttributes.copies
import de.gmuth.ipp.client.IppTemplateAttributes.documentFormat
import de.gmuth.ipp.client.IppTemplateAttributes.finishings
import de.gmuth.ipp.client.IppTemplateAttributes.jobName
import de.gmuth.ipp.client.IppTemplateAttributes.jobPriority
import de.gmuth.ipp.client.IppTemplateAttributes.media
import de.gmuth.ipp.client.IppTemplateAttributes.numberUp
import de.gmuth.ipp.client.IppTemplateAttributes.pageRanges
import de.gmuth.ipp.client.IppTemplateAttributes.printerResolution
import de.gmuth.ipp.client.IppWhichJobs.Completed
import de.gmuth.ipp.core.IppOperation.GetPrinterAttributes
import de.gmuth.log.Logging
import de.gmuth.log.Logging.LogLevel.TRACE
import java.io.File
import java.io.FileInputStream
import java.net.URI
import kotlin.io.path.createTempDirectory
import kotlin.io.path.pathString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IppPrinterTests {

    companion object {
        val log = Logging.getLogger {}
        val blankPdf = File("tool/A4-blank.pdf")
    }

    init {
        IppClient.log.logLevel = TRACE
        IppPrinter.log.logLevel = TRACE
    }

    val httpClient = HttpClientMock()
    val ippConfig = IppConfig()

    val printer = IppPrinter(
        URI.create("ipp://printer"),
        ippClient = IppClient(ippConfig, httpClient),
        getPrinterAttributesOnInit = false
    ).apply {
        attributes = File("printers/Simulated_Laser_Printer/Get-Printer-Attributes.ipp")
            .toIppResponse()
            .printerGroup
    }

    @Test
    fun printerAttributes() {
        printer.apply {
            log.logLevel = TRACE
            log.info { toString() }
            assertTrue(isAcceptingJobs)
            assertTrue(documentFormatSupported.contains("application/pdf"))
            assertTrue(supportsOperations(GetPrinterAttributes))
            assertTrue(isDuplexSupported())
            assertFalse(colorSupported)
            assertTrue(sidesSupported.contains("two-sided-short-edge"))
            assertTrue(mediaSupported.contains("iso_a4_210x297mm"))
            assertTrue(mediaReady.contains("iso_a4_210x297mm"))
            assertEquals("na_letter_8.5x11in", mediaDefault)
            assertTrue(supportsVersion("1.1"))
            assertEquals(URI.create("urf:///20"), deviceUri)
            assertTrue(hasCapability(CanPunchOutput))
            marker(CupsMarker.Color.BLACK).apply {
                assertEquals(80, level)
                assertEquals(10, lowLevel)
                assertEquals(100, highLevel)
                assertEquals("toner", type)
                assertEquals("Black", name)
                assertEquals(80, levelPercent())
                assertEquals("#000000", colorCode)
                assertFalse(levelIsLow())
                log.info { this }
            }
            assertTrue(isIdle())
            assertFalse(isProcessing())
            assertFalse(isStopped())
            assertFalse(isMediaNeeded())
            assertFalse(isCups())
            printerType.apply {
                log.info { toString() }
                logDetails()
            }
            communicationChannelsSupported.forEach {
                log.info { "${it.uri}, ${it.security}, ${it.authentication}, $it" }
            }
            ippConfig.logDetails()
        }
    }

    @Test
    fun savePrinterAttributes() {
        httpClient.mockResponse("Simulated_Laser_Printer/Get-Printer-Attributes.ipp")
        printer.savePrinterAttributes(createTempDirectory().pathString)
    }

    @Test
    fun updateAttributes() {
        httpClient.mockResponse("Simulated_Laser_Printer/Get-Printer-Attributes.ipp")
        printer.apply {
            updateAttributes()
            logDetails()
            assertEquals(122, attributes.size)
        }
    }

    @Test
    fun validateJob() {
        printer.validateJob(
            documentFormat("application/pdf"),
            media("iso_a4_210x297mm"),
        )
    }

    @Test
    fun printJobFile() {
        httpClient.mockResponse("Simulated_Laser_Printer/Print-Job.ipp")
        printer.printJob(
            File("tool/A4-blank.pdf"),
            jobName("A4.pdf"),
            documentFormat("application/pdf"),
            media("iso_a4_210x297mm"),
            jobPriority(30),
            copies(1),
            numberUp(1),
            pageRanges(1..5),
            printerResolution(600),
            IppOrientationRequested.Portrait,
            IppColorMode.Monochrome,
            IppSides.TwoSidedLongEdge,
            IppPrintQuality.High,
            finishings(Staple, Punch),
            IppMedia.Collection(
                size = IppMedia.Size(20, 30),
                margins = IppMedia.Margins(10, 10, 10, 10),
                source = "main",
                type = "stationery"
            )
        ).apply {
            assertEquals(461881017, id)
            assertEquals(IppJobState.Pending, state)
            assertEquals("pending", state.toString())
            assertTrue(stateReasons.contains("none"))
        }
    }

    @Test
    fun printJobInputStream() {
        httpClient.mockResponse("Simulated_Laser_Printer/Print-Job.ipp")
        printer.printJob(FileInputStream(blankPdf))
    }

    @Test
    fun printJobByteArray() {
        httpClient.mockResponse("Simulated_Laser_Printer/Print-Job.ipp")
        printer.printJob(blankPdf.readBytes())
    }

    @Test
    fun printUri() {
        httpClient.mockResponse("Simulated_Laser_Printer/Print-Job.ipp")
        printer.printUri(URI.create("http://server/document.pdf"))
    }

    @Test
    fun createJob() {
        httpClient.mockResponse("Simulated_Laser_Printer/Print-Job.ipp")
        printer.createJob().apply {
            httpClient.mockResponse("Simulated_Laser_Printer/Print-Job.ipp")
            sendDocument(FileInputStream(blankPdf), true, "blank.pdf", "en")
            httpClient.mockResponse("Simulated_Laser_Printer/Print-Job.ipp")
            sendUri(URI.create("http://server/document.pdf"), true, "black.pdf", "en")
        }
    }

    @Test
    fun getJob() {
        httpClient.mockResponse("Simulated_Laser_Printer/Get-Job-Attributes.ipp")
        printer.getJob(11).apply {
            assertEquals(21, attributes.size)
        }
    }

    @Test
    fun getJobsWithDefaultParameters() {
        httpClient.mockResponse("Simulated_Laser_Printer/Get-Jobs.ipp")
        printer.getJobs().apply {
            assertEquals(1, size)
        }
    }

    @Test
    fun getJobsWithParameters() {
        httpClient.mockResponse("Simulated_Laser_Printer/Get-Jobs.ipp")
        printer.getJobs(Completed, myJobs = true, limit = 10).apply {
            assertEquals(1, size)
        }
    }

    @Test
    fun sound() {
        printer.sound()
    }

    @Test
    fun flash() {
        printer.flash()
    }

    @Test
    fun pause() {
        printer.ippClient.basicAuth("user", "password")
        printer.pause()
    }

    @Test
    fun resume() {
        printer.resume()
    }

    @Test
    fun purgeJobs() {
        printer.purgeJobs()
    }

}