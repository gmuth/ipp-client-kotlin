package de.gmuth.ipp.client

/**
 * Copyright (c) 2021 Gerhard Muth
 */

import de.gmuth.http.HttpClientMock
import de.gmuth.ipp.core.IppResponse
import de.gmuth.ipp.core.IppStatus.SuccessfulOk
import de.gmuth.ipp.core.IppTag
import de.gmuth.ipp.core.IppTag.*
import de.gmuth.ipp.core.toIppString
import de.gmuth.log.Logging
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IppJobTests {

    companion object {
        val log = Logging.getLogger(Logging.LogLevel.INFO) {}
        val blankPdf = File("tool/A4-blank.pdf")
    }

    val httpClient = HttpClientMock()
    val printer: IppPrinter
    val job: IppJob

    init {
        // mock ipp printer
        printer = IppPrinter(
                URI.create("ipp://printer"),
                httpClient = httpClient,
                ippConfig = IppConfig(getPrinterAttributesOnInit = false)
        ).apply {
            attributes = ippResponse("Get-Printer-Attributes.ipp").printerGroup
        }
        // mock ipp job
        job = IppJob(printer, ippResponse("Get-Job-Attributes.ipp").jobGroup.apply {
            attribute("document-name-supplied", NameWithLanguage, "blank.pdf".toIppString())
        })
    }

    fun ippResponse(fileName: String, directory: String = "printers/CUPS_HP_LaserJet_100_color_MFP_M175") =
            IppResponse().apply { read(File(directory, fileName)) }

    @Test
    fun jobAttributes() {
        job.apply {
            log.info { toString() }
            logDetails()
            assertEquals("ipp://localhost:631/jobs/2366", uri.toString())
            assertEquals(0, mediaSheetsCompleted)
            assertEquals(2, kOctets)
            assertEquals(1, numberOfDocuments)
            assertEquals("blank.pdf", documentNameSupplied.text)
            assertTrue(hasStateReasons())
            assertFalse(isProcessing())
            assertFalse(isProcessingStopped())
            assertTrue(isTerminated())
            assertFalse(isProcessingToStopPoint())
        }
    }

    @Test
    fun getAttributes() {
        httpClient.ippResponse = ippResponse("Get-Job-Attributes.ipp")
        job.getJobAttributes()
    }

    @Test
    fun updateAttributes() {
        httpClient.ippResponse = ippResponse("Get-Job-Attributes.ipp")
        job.apply {
            updateAttributes()
            logDetails()
            assertEquals(31, attributes.size)
        }
    }

    @Test
    fun hold() {
        httpClient.ippResponse = ippResponse("Get-Job-Attributes.ipp")
        job.hold()
    }

    @Test
    fun release() {
        httpClient.ippResponse = ippResponse("Get-Job-Attributes.ipp")
        job.release()
    }

    @Test
    fun restart() {
        httpClient.ippResponse = ippResponse("Get-Job-Attributes.ipp")
        job.restart()
    }

    @Test
    fun cancel() {
        httpClient.ippResponse = ippResponse("Get-Job-Attributes.ipp")
        job.cancel()
    }

    @Test
    fun cancelWithMessage() {
        httpClient.ippResponse = ippResponse("Get-Job-Attributes.ipp")
        job.cancel("message")
    }

    @Test
    fun isProcessing() {
        job.apply {
            attributes.attribute("job-state", IppTag.Enum, IppJobState.Processing.code)
            assertTrue(isProcessing())
            assertFalse(isTerminated())
        }
    }

    @Test
    fun isProcessingStopped() {
        job.apply {
            attributes.attribute("job-state", IppTag.Enum, IppJobState.ProcessingStopped.code)
            assertTrue(isProcessingStopped())
        }
    }

    @Test
    fun isProcessingToStopPoint() {
        job.apply {
            attributes.remove("job-state-reasons")
            assertFalse(isProcessingToStopPoint())
            attributes.attribute("job-state-reasons", Keyword, "processing-to-stop-point")
            assertTrue(isProcessingToStopPoint())
            httpClient.ippResponse = ippResponse("Get-Job-Attributes.ipp")
            cancel()
        }
    }

    @Test
    fun sendDocument() {
        httpClient.mockResponse("Simulated_Laser_Printer/Print-Job.ipp")
        job.sendDocument(FileInputStream(blankPdf))
    }

    @Test
    fun sendUri() {
        httpClient.mockResponse("Simulated_Laser_Printer/Print-Job.ipp")
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
                remove("job-impressions-completed")
            }
            assertEquals("Job #2366:", toString())
        }
    }

    @Test
    fun printerState() {
        assertEquals(IppPrinterState.Idle, job.printer.state)
    }

    fun cupsDocumentResponse(format: String) = IppResponse(SuccessfulOk).apply {
        createAttributesGroup(Job).apply {
            attribute("document-format", MimeMediaType, format)
            attribute("document-number", Integer, 1)
            attribute("document-name", NameWithoutLanguage, "cups-doc".toIppString())
        }
        documentInputStream = FileInputStream(blankPdf)
    }

    @Test
    fun cupsGetDocument1() {
        job.attributes.remove("number-of-documents")
        httpClient.ippResponse = cupsDocumentResponse("application/pdf")
        job.cupsGetDocument().apply {
            log.info { toString() }
            save()
        }
    }

    @Test
    fun cupsGetDocument2() {
        httpClient.ippResponse = cupsDocumentResponse("application/postscript")
        job.cupsGetDocument().filename()
    }

    @Test
    fun cupsGetDocument3() {
        IppDocument.log.logLevel = Logging.LogLevel.DEBUG
        printer.attributes.remove("cups-version")
        httpClient.ippResponse = cupsDocumentResponse("application/octetstream").apply {
            jobGroup.remove("document-name")
        }
        job.cupsGetDocument(2).apply {
            log.info { toString() }
            log.info { "${filename()} (${readBytes().size} bytes)" }

            job.attributes.remove("document-name-supplied")
            log.info { "${filename()}" }
        }
    }

}