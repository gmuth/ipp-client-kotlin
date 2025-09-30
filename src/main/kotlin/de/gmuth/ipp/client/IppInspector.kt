package de.gmuth.ipp.client

/**
 * Copyright (c) 2023-2025 Gerhard Muth
 */

import de.gmuth.ipp.attributes.*
import de.gmuth.ipp.attributes.TemplateAttributes.jobName
import de.gmuth.ipp.core.IppOperation.*
import java.net.URI
import java.nio.file.Path
import java.util.logging.Logger.getLogger

class IppInspector {

    companion object {
        const val pdfA4 = "blank_A4.pdf"
        var inspectorDirectory: Path = Path.of("inspected-printers")
        private val logger = getLogger(IppInspector::class.java.name)
        private fun getModel(printerUri: URI) = StringBuilder().run {
            // use another IppPrinter instance to leave request-id-counter untouched
            with(IppPrinter(printerUri, getPrinterAttributesOnInit = false)) {
                updateAttributes("cups-version", "printer-make-and-model")
                if (isCups()) append("CUPS_")
                append(makeAndModel.text.replace("\\s+".toRegex(), "_"))
            }
            toString()
        }
    }

    fun inspect(printerUri: URI, cancelJob: Boolean = true) =
        IppPrinter(printerUri, getPrinterAttributesOnInit = false).inspect(cancelJob)

    /**
     * Exchange a few IPP requests and save the IPP responses returned by the printer.
     * Operations:
     * - Get-Printer-Attributes
     * - Print-Job, Get-Jobs, Get-Job-Attributes
     * - Hold-Job, Release-Job, Cancel-Job
     */
    private fun IppPrinter.inspect(cancelJob: Boolean) {
        logger.info { "Inspect printer $printerUri" }

        val printerModel = getModel(printerUri)
        logger.info { "Printer model: $printerModel" }
        printerDirectory = inspectorDirectory.resolve(printerModel)

        ippConfig.userName = "ipp-inspector"
        ippClient.saveEvents = true
        ippClient.saveMessages = true
        ippClient.saveMessagesDirectory = printerDirectory

        logger.info { "> Get printer attributes" }
        updateAttributes()

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

        if (attributes.containsKey("printer-icons")) {
            logger.info { "> Save printer icons" }
            savePrinterIcons()
        }

        ippClient.saveMessages = false
        if(attributes.containsKey("printer-strings-languages-supported")) {
            logger.info { "> Save all printer strings" }
            saveAllPrinterStrings()
        } else if(attributes.containsKey("printer-strings-uri")) {
            attributes.getValueOrNull<String>("natural-language-configured")?.also {
                logger.info { "> Save printer strings for configured language $it"}
                savePrinterStrings(it)
            }
        }
        ippClient.saveMessages = true

        if (supportsOperations(CupsGetPPD)) {
            logger.info { "> CUPS Get PPD" }
            savePPD(filename = "$printerModel.ppd")
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

        logger.info { "> Print job" }
        val job = printPdf()
        logger.info { "$job" }

        logger.info { "> Get jobs" }
        getJobs().forEach { logger.info { "$it" } }

        job.inspect(cancelJob)
    }

    private fun IppPrinter.printPdf(): IppJob {

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

        val documentStream = javaClass.getResourceAsStream("/$pdfResource")!!
        return printJob(documentStream, jobName(pdfResource))
    }

    private fun IppJob.inspect(cancelJob: Boolean) {

        if (printer.supportsOperations(HoldJob, ReleaseJob)) {
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

        printer.ippClient.saveMessages = false
        logger.info { "> Wait for termination" }
        waitForTermination()

        if (!cancelJob) {
            printer.ippClient.saveMessages = true
            logger.info { "> Get last attributes of terminated job" }
            updateAttributes()
        }
    }
}