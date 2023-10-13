package de.gmuth.ipp.client

/**
 * Copyright (c) 2023 Gerhard Muth
 */

import de.gmuth.ipp.attributes.*
import de.gmuth.ipp.attributes.TemplateAttributes.jobName
import de.gmuth.ipp.core.IppOperation.*
import java.io.File
import java.net.URI
import java.util.logging.Logger.getLogger

object IppInspector {
    private val log = getLogger(javaClass.name)
    var directory: String = "inspected-printers"

    const val pdfA4 = "blank_A4.pdf"

    fun inspect(printerUri: URI, cancelJob: Boolean = true) =
        IppPrinter(printerUri).inspect(cancelJob)

    /**
     * Exchange a few IPP requests and save the IPP responses returned by the printer.
     * Operations:
     * - Get-Printer-Attributes
     * - Print-Job, Get-Jobs, Get-Job-Attributes
     * - Hold-Job, Release-Job, Cancel-Job
     */
    private fun IppPrinter.inspect(cancelJob: Boolean) {
        log.info { "Inspect printer $printerUri" }

        val printerModel = with(StringBuilder()) {
            if (isCups()) append("CUPS-")
            append(makeAndModel.text.replace("\\s+".toRegex(), "_"))
            toString()
        }
        log.info { "Printer model: $printerModel" }

        ippClient.saveMessages = true
        ippClient.saveMessagesDirectory = File(directory, printerModel).createDirectoryIfNotExists()
        workDirectory = ippClient.saveMessagesDirectory

        attributes.run {
            // Media
            if (containsKey("media-supported")) log.info { "Media supported: $mediaSupported" }
            if (containsKey("media-ready")) log.info { "Media ready: $mediaReady" }
            if (containsKey("media-default")) log.info { "Media default: $mediaDefault" }
            // URIs
            log.info { "Communication channels supported:" }
            communicationChannelsSupported.forEach { log.info { "  $it" } }
        }

        val pdfResource = when {
            !attributes.containsKey("media-ready") -> {
                log.warning { "media-ready not supported" }
                pdfA4
            }

            mediaReady.contains("iso-a4") || mediaReady.contains("iso_a4_210x297mm") -> pdfA4
            mediaReady.contains("na_letter") || mediaReady.contains("na_letter_8.5x11in") -> "blank_USLetter.pdf"
            else -> {
                log.warning { "No PDF available for media '$mediaReady', trying A4" }
                pdfA4
            }
        }

        ippConfig.userName = "ipp-inspector"
        runInspectionWorkflow(pdfResource, cancelJob)
    }

    private fun IppPrinter.runInspectionWorkflow(pdfResource: String, cancelJob: Boolean) {

        log.info { "> Get printer attributes" }
        getPrinterAttributes()

        if (supportsOperations(CupsGetPPD)) {
            log.info { "> CUPS Get PPD" }
            savePPD(file = File(workDirectory, "0-${name.text}.ppd"))
        }

        if (supportsOperations(IdentifyPrinter)) {
            val action = with(identifyActionsSupported) { if (contains("sound")) "sound" else first() }
            log.info { "> Identify by $action" }
            identify(action)
        }

        log.info { "> Validate job" }
        val response = try {
            validateJob(
                jobName("Validation"),
                DocumentFormat.OCTET_STREAM,
                Sides.TwoSidedShortEdge,
                PrintQuality.Normal,
                ColorMode.Color,
                Media.ISO_A3
            )
        } catch (ippExchangeException: IppExchangeException) {
            ippExchangeException.response
        }
        log.info { response.toString() }

        log.info { "> Print job $pdfResource" }
        printJob(
            IppInspector::class.java.getResourceAsStream("/$pdfResource"),
            jobName(pdfResource),

            ).run {
            log.info { toString() }

            log.info { "> Get jobs" }
            for (job in getJobs()) {
                log.info { "$job" }
            }

            if (supportsOperations(HoldJob, ReleaseJob)) {
                log.info { "> Hold job" }
                hold()
                log.info { "> Release job" }
                release()
            }

            if (cancelJob) {
                log.info { "> Cancel job" }
                cancel()
            }

            log.info { "> Update job attributes" }
            updateAttributes()

            ippClient.saveMessages = false
            log.info { "> Wait for termination" }
            waitForTermination()
        }
    }
}