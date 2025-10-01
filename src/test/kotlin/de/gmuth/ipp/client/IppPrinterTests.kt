package de.gmuth.ipp.client

/**
 * Copyright (c) 2021-2024 Gerhard Muth
 */

import de.gmuth.ipp.attributes.*
import de.gmuth.ipp.attributes.Finishing.Punch
import de.gmuth.ipp.attributes.Finishing.Staple
import de.gmuth.ipp.attributes.Orientation.Portrait
import de.gmuth.ipp.attributes.PrinterType.Capability.CanPunchOutput
import de.gmuth.ipp.attributes.TemplateAttributes.copies
import de.gmuth.ipp.attributes.TemplateAttributes.finishings
import de.gmuth.ipp.attributes.TemplateAttributes.jobName
import de.gmuth.ipp.attributes.TemplateAttributes.jobPriority
import de.gmuth.ipp.attributes.TemplateAttributes.numberUp
import de.gmuth.ipp.attributes.TemplateAttributes.orientationRequested
import de.gmuth.ipp.attributes.TemplateAttributes.outputBin
import de.gmuth.ipp.attributes.TemplateAttributes.pageRanges
import de.gmuth.ipp.attributes.TemplateAttributes.printerResolution
import de.gmuth.ipp.client.WhichJobs.Completed
import de.gmuth.ipp.core.IppOperation.GetPrinterAttributes
import java.io.File
import java.io.FileInputStream
import java.net.URI
import java.util.logging.Logger.getLogger
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IppPrinterTests {

    private val logger = getLogger(javaClass.name)
    val blankPdf = File("tool/A4-blank.pdf")
    val ippClientMock = IppClientMock("printers/Simulated_Laser_Printer")
    val printer = IppPrinter(
        URI.create("ipp://printer-for-printer-tests:6310"),
        ippClient = ippClientMock.apply { mockResponse("Get-Printer-Attributes.ipp") }
    )

    @Test
    fun printerAttributes() {
        printer.run {
            logger.info { toString() }
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
            assertEquals(geoLocation, Pair(37.33182, 122.03118))
            marker(Marker.Color.BLACK).apply {
                assertEquals(80, level)
                assertEquals(10, lowLevel)
                assertEquals(100, highLevel)
                assertEquals("toner", type)
                assertEquals("Black", name)
                assertEquals(80, levelPercent())
                assertEquals("#000000", colorCode)
                assertFalse(levelIsLow())
                logger.info { this.toString() }
            }
            assertTrue(isIdle())
            assertFalse(isProcessing())
            assertFalse(isStopped())
            assertFalse(isMediaNeeded())
            assertFalse(isCups())
            printerType.apply {
                logger.info { toString() }
                log(logger)
            }
            communicationChannelsSupported.forEach {
                logger.info { "${it.uri}, ${it.security}, ${it.authentication}, $it" }
            }
            ippConfig.log(logger)
        }
    }

    @Test
    fun savePrinterAttributes() {
        ippClientMock.mockResponse("Get-Printer-Attributes.ipp")
        printer.printerDirectory = createTempDirectory()
        printer.savePrinterAttributes()
    }

    @Test
    fun updateAttributes() {
        ippClientMock.mockResponse("Get-Printer-Attributes.ipp")
        printer.run {
            updateAttributes()
            log(logger)
            assertEquals(122, attributes.size)
        }
    }

    @Test
    fun validateJob() {
        printer.validateJob(
            DocumentFormat.PDF,
            Media.ISO_A4
        )
    }

    @Test
    fun printJobFile() {
        ippClientMock.mockResponse("Print-Job.ipp")
        printer.printJob(
            File("tool/A4-blank.pdf"),
            jobName("A4.pdf"),
            DocumentFormat("application/pdf"),
            Media("iso_a4_210x297mm"),
            jobPriority(30),
            copies(1),
            numberUp(1),
            outputBin("tray-2"),
            pageRanges(1..5),
            printerResolution(600),
            orientationRequested(Portrait),
            ColorMode.Monochrome,
            Sides.TwoSidedLongEdge,
            PrintQuality.High,
            finishings(Staple, Punch),
            MediaCollection(
                size = MediaSize(20, 30),
                margin = MediaMargin(10, 10, 10, 10),
                source = MediaSource("main"),
                type = "stationery"
            )
        ).apply {
            assertEquals(461881017, id)
            assertEquals(JobState.Pending, state)
            assertEquals("pending", state.toString())
            assertTrue(stateReasons.contains("none"))
        }
    }

    @Test
    fun printJobInputStream() {
        ippClientMock.mockResponse("Print-Job.ipp")
        printer.printJob(FileInputStream(blankPdf)).run {
            log(logger)
            assertEquals(461881017, id)
            assertEquals(JobState.Pending, state)
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
        printer.basicAuth("user", "password")
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