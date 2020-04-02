package de.gmuth.ipp.print

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.client.IppClient
import de.gmuth.ipp.client.IppPrintJob
import de.gmuth.ipp.core.IppTag
import de.gmuth.ipp.core.toIppJob
import de.gmuth.print.ColorMode
import de.gmuth.print.PrintService
import java.io.File
import java.net.URI

class IppPrintService(private val printerUri: URI) : PrintService {

    private val ippClient = IppClient()

    override fun printFile(file: File, colorMode: ColorMode, waitForTermination: Boolean) {

        val request = IppPrintJob(printerUri, file = file)
        request.jobGroup.attribute("output-mode", IppTag.Keyword, ippColorMode(colorMode)) // CUPS extension
        request.logDetails("IPP: ")

        val response = ippClient.exchangeSuccessful(
                printerUri, request, "PrintJob '$file' failed", request.documentInputStream
        )

        val job = response.jobGroup.toIppJob()
        if (waitForTermination) {
            ippClient.waitForTermination(job)
        }
        job.logDetails()
    }

    // API MAPPING

    fun ippColorMode(colorMode: ColorMode) = when (colorMode) {
        ColorMode.Auto -> "auto"
        ColorMode.Color -> "color"
        ColorMode.Monochrome -> "monochrome"
    }

}