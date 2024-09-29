package de.gmuth.ipp.client

/**
 * Copyright (c) 2021-2024 Gerhard Muth
 */

import de.gmuth.ipp.attributes.JobState
import de.gmuth.ipp.attributes.PrinterState
import de.gmuth.ipp.core.IppResponse
import de.gmuth.ipp.core.IppStatus.SuccessfulOk
import de.gmuth.ipp.core.IppString
import de.gmuth.ipp.core.IppTag
import de.gmuth.ipp.core.IppTag.*
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.net.URI
import java.util.logging.Logger.getLogger
import kotlin.io.path.createTempDirectory
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IppJobTests {

    private val logger = getLogger(javaClass.name)
    val blankPdf = File("tool/A4-blank.pdf")

    val ippClientMock = IppClientMock("printers/CUPS_HP_LaserJet_100_color_MFP_M175")

    val printer = IppPrinter(
        URI.create("ipp://printer-for-job-tests"),
        ippClient = ippClientMock.apply { mockResponse("Get-Printer-Attributes.ipp") }
    )

    val job: IppJob = printer.run {
        ippClientMock.mockResponse("Get-Job-Attributes.ipp")
        getJob(2366).apply {
            attributes.attribute("document-name-supplied", NameWithoutLanguage, IppString("blank.pdf"))
        }
    }

    @Test
    fun jobAttributes() {
        job.apply {
            logger.info { toString() }
            log(logger)
            assertEquals("ipp://localhost:631/jobs/2366", uri.toString())
            assertEquals(0, mediaSheetsCompleted)
            assertEquals(2, kOctets)
            assertEquals(1, numberOfDocuments)
            assertEquals("blank.pdf", documentNameSupplied.text)
            assertFalse(isProcessing())
            assertFalse(isProcessingStopped())
            assertTrue(isTerminated())
            assertFalse(isProcessingToStopPoint())
        }
    }

    @Test
    fun getAttributes() {
        job.getJobAttributes()
    }

    @Test
    fun updateAttributes() {
        job.apply {
            updateAttributes()
            log(logger)
            assertEquals(32, attributes.size)
        }
    }

    @Test
    fun hold() {
        job.hold()
    }

    @Test
    fun release() {
        job.release()
    }

    @Test
    fun restart() {
        job.restart()
    }

    @Test
    fun cancel() {
        job.cancel()
    }

    @Test
    fun cancelWithMessage() {
        job.cancel("message")
    }

    @Test
    fun isProcessing() {
        job.apply {
            attributes.attribute("job-state", IppTag.Enum, JobState.Processing.code)
            assertTrue(isProcessing())
            assertFalse(isTerminated())
        }
    }

    @Test
    fun isProcessingStopped() {
        job.apply {
            attributes.attribute("job-state", IppTag.Enum, JobState.ProcessingStopped.code)
            assertTrue(isProcessingStopped())
        }
    }

    @Test
    fun isProcessingToStopPoint() {
        job.apply {
            attributes.attribute("job-state-reasons", Keyword, "none")
            assertFalse(isProcessingToStopPoint())
            attributes.attribute("job-state-reasons", Keyword, "processing-to-stop-point")
            assertTrue(isProcessingToStopPoint())
            cancel()
        }
    }

    @Test
    fun sendDocument() {
        ippClientMock.mockResponse("Print-Job.ipp")
        job.sendDocument(FileInputStream(blankPdf))
    }

    @Test
    fun sendUri() {
        ippClientMock.mockResponse("Print-Job.ipp")
        job.sendUri(URI.create("ftp://no.doc"))
    }

    @Test
    fun jobToString() {
        job.apply {
            attributes.apply {
                remove("job-state")
                remove("job-state-reasons")
                remove("job-name")
                remove("job-originating-user-name")
                remove("job-originating-host-name")
                remove("job-impressions-completed")
                remove("number-of-documents")
                remove("job-printer-uri")
                remove("job-uri")
            }
            assertEquals("Job #2366", toString())
        }
    }

    @Test
    fun printerState() {
        assertEquals(PrinterState.Idle, job.printer.state)
    }

    fun cupsDocumentResponse(format: String) = IppResponse(SuccessfulOk).apply {
        createAttributesGroup(Job).apply {
            attribute("document-format", MimeMediaType, format)
            attribute("document-number", Integer, 1)
            attribute("document-name", NameWithoutLanguage, "cups-doc")
        }
        documentInputStream = FileInputStream(blankPdf)
    }

    @Test
    fun cupsGetDocument1() {
        ippClientMock.mockResponse(cupsDocumentResponse("application/pdf"))

        job.cupsGetDocument().apply {
            logger.info { toString() }
            log(logger)
            save(directory = createTempDirectory().toFile()).delete()
            assertEquals("job-2366-gmuth-A4-blank.pdf", filename())
        }
    }

    @Test
    fun cupsGetDocument2() {
        ippClientMock.mockResponse(cupsDocumentResponse("application/postscript"))
        job.cupsGetDocument().run {
            assertEquals("ps", filenameExtension())
        }
    }

    @Test
    fun cupsGetDocument3() {
        printer.attributes.remove("cups-version")
        ippClientMock.mockResponse(cupsDocumentResponse("application/octet-stream").apply {
            jobGroup.remove("document-name")
        })
        job.cupsGetDocument(2).apply {
            logger.info { toString() }
            logger.info { "${filename()} (${readBytes().size} bytes)" }
            job.attributes.remove("document-name-supplied")
            logger.info { filename() }
            assertEquals("bin", filenameExtension())
        }
    }

    @Test
    fun cupsGetAndSaveDocuments() {
        ippClientMock.mockResponse(cupsDocumentResponse("application/postscript"))
        job.cupsGetDocuments(save = true, directory = createTempDirectory().toFile())
    }

    @Test
    fun cupsGetDocumentsWithoutSaving() {
        ippClientMock.mockResponse(cupsDocumentResponse("application/postscript"))
        job.cupsGetDocuments()
    }

}