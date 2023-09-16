package de.gmuth.ipp.client

/**
 * Copyright (c) 2021-2023 Gerhard Muth
 */

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
import java.io.File
import java.io.FileInputStream
import java.net.URI
import java.util.logging.Logger.getLogger
import kotlin.io.path.createTempDirectory
import kotlin.io.path.pathString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IppPrinterTests {

    val tlog = getLogger(javaClass.name)
    val blankPdf = File("tool/A4-blank.pdf")
    val ippClientMock = IppClientMock("printers/Simulated_Laser_Printer")
    val printer = IppPrinter(
        URI.create("ipp://printer-for-printer-tests"),
        ippClient = ippClientMock.apply { mockResponse("Get-Printer-Attributes.ipp") }
    )

    @Test
    fun printerAttributes() {
        printer.run {
            tlog.info { toString() }
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
                log.info { this.toString() }
            }
            assertTrue(isIdle())
            assertFalse(isProcessing())
            assertFalse(isStopped())
            assertFalse(isMediaNeeded())
            assertFalse(isCups())
            printerType.apply {
                tlog.info { toString() }
                log(tlog)
            }
            communicationChannelsSupported.forEach {
                tlog.info { "${it.uri}, ${it.security}, ${it.authentication}, $it" }
            }
            ippConfig.log(tlog)
        }
    }

    @Test
    fun savePrinterAttributes() {
        ippClientMock.mockResponse("Get-Printer-Attributes.ipp")
        printer.savePrinterAttributes(createTempDirectory().pathString)
    }

    @Test
    fun updateAttributes() {
        ippClientMock.mockResponse("Get-Printer-Attributes.ipp")
        printer.run {
            updateAttributes()
            log(tlog)
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
        ippClientMock.mockResponse("Print-Job.ipp")
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
        ippClientMock.mockResponse("Print-Job.ipp")
        printer.printJob(FileInputStream(blankPdf)).run {
            log(tlog)
            assertEquals(461881017, id)
            assertEquals(IppJobState.Pending, state)
            assertEquals(listOf("none"), stateReasons)
            assertEquals("ipp://SpaceBook-2.local.:8632/jobs/461881017", uri.toString())
        }
    }

    @Test
    fun printJobByteArray() {
        ippClientMock.mockResponse("Print-Job.ipp")
        printer.printJob(blankPdf.readBytes())
    }

    @Test
    fun printUri() {
        ippClientMock.mockResponse("Print-Job.ipp")
        printer.printUri(URI.create("http://server/document.pdf"))
    }

    @Test
    fun createJob() {
        ippClientMock.mockResponse("Print-Job.ipp")
        printer.createJob().run {
            sendDocument(FileInputStream(blankPdf), true, "blank.pdf", "en")
            sendUri(URI.create("http://server/document.pdf"), true, "black.pdf", "en")
        }
    }

    @Test
    fun getJob() {
        ippClientMock.mockResponse("Get-Job-Attributes.ipp")
        printer.getJob(11).run {
            assertEquals(21, attributes.size)
        }
    }

    @Test
    fun getJobsWithDefaultParameters() {
        ippClientMock.mockResponse("Get-Jobs.ipp")
        printer.getJobs().run {
            assertEquals(1, size)
        }
    }

    @Test
    fun getJobsWithParameters() {
        ippClientMock.mockResponse("Get-Jobs.ipp")
        printer.getJobs(Completed, myJobs = true, limit = 10).run {
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