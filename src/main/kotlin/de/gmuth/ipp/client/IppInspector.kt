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
    private val logger = getLogger(javaClass.name)
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
        logger.info { "Inspect printer $printerUri" }

        val printerModel = with(StringBuilder()) {
            if (isCups()) append("CUPS-")
            append(makeAndModel.text.replace("\\s+".toRegex(), "_"))
            toString()
        }
        logger.info { "Printer model: $printerModel" }

        ippClient.saveMessages = true
        ippClient.saveMessagesDirectory = File(directory, printerModel).createDirectoryIfNotExists()
        workDirectory = ippClient.saveMessagesDirectory

        attributes.run {
            // Media
            if (containsKey("media-supported")) logger.info { "Media supported: $mediaSupported" }
            if (containsKey("media-ready")) logger.info { "Media ready: $mediaReady" }
            if (containsKey("media-default")) logger.info { "Media default: $mediaDefault" }
            // URIs
            logger.info { "Communication channels supported:" }
            communicationChannelsSupported.forEach { logger.info { "  $it" } }
            logger.info { "Document formats: $documentFormatSupported" }
        }

        val pdfResource = when {
            !attributes.containsKey("media-ready") -> {
                logger.warning { "media-ready not supported" }
                pdfA4
            }

            mediaReady.contains("iso-a4") || mediaReady.contains("iso_a4_210x297mm") -> pdfA4
            mediaReady.contains("na_letter") || mediaReady.contains("na_letter_8.5x11in") -> "blank_USLetter.pdf"
            else -> {
                logger.warning { "No PDF available for media '$mediaReady', trying A4" }
                pdfA4
            }
        }

        ippConfig.userName = "ipp-inspector"
        runInspectionWorkflow(pdfResource, cancelJob)
    }

    private fun IppPrinter.runInspectionWorkflow(pdfResource: String, cancelJob: Boolean) {

        logger.info { "> Get printer attributes" }
        getPrinterAttributes()

        if (supportsOperations(CupsGetPPD)) {
            logger.info { "> CUPS Get PPD" }
            savePPD(file = File(workDirectory, "0-${name.text}.ppd"))
        }

        if (supportsOperations(IdentifyPrinter)) {
            val action = with(identifyActionsSupported) { if (contains("sound")) "sound" else first() }
            logger.info { "> Identify by $action" }
            identify(action)
        }

        logger.info { "> Validate job" }
        val response = try {
            validateJob(
                jobName("Validation"),
                DocumentFormat.OCTET_STREAM,
                Sides.TwoSidedShortEdge,
                PrintQuality.Normal,
                ColorMode.Color,
                Media.ISO_A3
            )
        } catch (ippOperationException: IppOperationException) {
            ippOperationException.response
        }
        logger.info { response.toString() }

        logger.info { "> Print job $pdfResource" }
        printJob(
            IppInspector::class.java.getResourceAsStream("/$pdfResource")!!,
            jobName(pdfResource),

            ).run {
            logger.info { toString() }

            logger.info { "> Get jobs" }
            for (job in getJobs()) {
                logger.info { "$job" }
            }

            if (supportsOperations(HoldJob, ReleaseJob)) {
                logger.info { "> Hold job" }
                hold()
                logger.info { "> Release job" }
                release()
            }

            if (cancelJob) {
                logger.info { "> Cancel job" }
                cancel()
            }

            logger.info { "> Update job attributes" }
            updateAttributes()

            ippClient.saveMessages = false
            logger.info { "> Wait for termination" }
            waitForTermination()
        }
    }
}