package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.http.Http
import de.gmuth.ipp.core.*
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URI

class IppPrinter(val printerUri: URI) {

    val ippClient = IppClient()

    val attributes = getAttributes()

    var httpAuth: Http.Auth?
        get() = ippClient.httpAuth
        set(value) {
            ippClient.httpAuth = value
        }

    //--------------
    // IppAttributes
    //--------------

    val name: IppString
        get() = attributes.getValue("printer-name")

    val makeAndModel: IppString
        get() = attributes.getValue("printer-make-and-model")

    val isAcceptingJobs: Boolean
        get() = attributes.getValue("printer-is-accepting-jobs")

    val state: IppPrinterState
        get() = IppPrinterState.fromInt(attributes.getValue("printer-state") as Int)

    val stateReasons: List<String>
        get() = attributes.getValues("printer-state-reasons")

    //--------------------------------------------------
    // Identify-Printer (e.g. flash,sound,display,speak)
    //--------------------------------------------------

    fun identify(action: String) {
        val request = ippRequest(IppOperation.IdentifyPrinter).apply {
            operationGroup.attribute("identify-actions", IppTag.Keyword, action)
        }
        exchangeSuccessful(request)
    }

    //--------------
    // Pause-Printer
    //--------------

    fun pause() {
        exchangeSuccessfulIppRequest(IppOperation.PausePrinter)
    }

    //---------------
    // Resume-Printer
    //---------------

    fun resume() {
        exchangeSuccessfulIppRequest(IppOperation.ResumePrinter)
    }

    //-----------------------
    // Get-Printer-Attributes
    //-----------------------

    fun getAttributes(requestedAttributes: List<String> = listOf()): IppAttributesGroup {
        val request = ippRequest(IppOperation.GetPrinterAttributes).apply {
            if (requestedAttributes.isNotEmpty()) {
                operationGroup.attribute("requested-attributes", IppTag.Keyword, requestedAttributes)
            }
        }
        return exchangeSuccessful(request).printerGroup
    }

    //-----------
    // PRINT FILE
    //-----------

    fun printFile(
            file: File,
            vararg jobParameters: IppJobParameter,
            documentFormat: String = "application/octet-stream",
            waitForTermination: Boolean = false

    ): IppJob {
        val request = printRequest(IppOperation.PrintJob, documentFormat, file.name, jobParameters.toList())
        val response = exchangeSuccessful(request, FileInputStream(file))
        return handlePrintResponse(response, waitForTermination)
    }

    //----------
    // PRINT URI
    //----------

    fun printUri(
            documentUri: URI,
            vararg jobParameters: IppJobParameter,
            documentFormat: String = "application/octet-stream",
            waitForTermination: Boolean = false

    ): IppJob {

        val request = printRequest(IppOperation.PrintUri, documentFormat, documentUri.path, jobParameters.toList())
        request.operationGroup.attribute("document-uri", IppTag.Uri, documentUri)
        val response = exchangeSuccessful(request)
        return handlePrintResponse(response, waitForTermination)
    }

    // ---- factory method for IppRequest with Operation Print-Job or Print-Uri

    private fun printRequest(
            printOperation: IppOperation,
            documentFormat: String,
            jobName: String,
            jobParameters: List<IppJobParameter>

    ) = ippRequest(printOperation).apply {
        operationGroup.attribute("job-name", IppTag.NameWithoutLanguage, jobName)
        operationGroup.attribute("document-format", IppTag.MimeMediaType, documentFormat)
        with(ippAttributesGroup(IppTag.Job)) {
            for (jobParameter in jobParameters) {
                put(jobParameter.toIppAttribute(attributes))
            }
        }
    }

    private fun handlePrintResponse(printResponse: IppResponse, wait: Boolean = false): IppJob {
        val job = IppJob(this, printResponse.jobGroup)
        if (wait) {
            job.waitForTermination()
        }
        job.logDetails()
        return job
    }

    //---------------------------
    // Get-Jobs (as List<IppJob>)
    //---------------------------

    fun getJobs(whichJobs: String? = null): List<IppJob> {
        val request = ippRequest(IppOperation.GetJobs)
        if (whichJobs != null) {
            request.operationGroup.attribute("which-jobs", IppTag.Keyword, whichJobs)
        }
        val response = exchangeSuccessful(request)
        val jobGroups = response.getAttributesGroups(IppTag.Job)
        return jobGroups.map {
            IppJob(this, it)
        }
    }

    //-------------------------------
    // Get-Job-Attributes (as IppJob)
    //-------------------------------

    fun getJob(jobId: Int): IppJob {
        val response = exchangeSuccessfulIppJobRequest(IppOperation.GetJobAttributes, jobId)
        return IppJob(this, response.jobGroup)
    }

    //----------------------
    // delegate to IppClient
    //----------------------

    fun ippRequest(operation: IppOperation) = with(ippClient) {
        ippRequest(operation, printerUri)
    }

    fun ippJobRequest(jobOperation: IppOperation, jobId: Int) = with(ippClient) {
        ippJobRequest(jobOperation, printerUri, jobId)
    }

    fun exchangeSuccessful(request: IppRequest, documentInputStream: InputStream? = null) = with(ippClient) {
        exchangeSuccessful(printerUri, request, documentInputStream)
    }

    fun exchangeSuccessfulIppRequest(operation: IppOperation) = with(ippClient) {
        exchangeSuccessful(printerUri, ippRequest(operation, printerUri))
    }

    fun exchangeSuccessfulIppJobRequest(operation: IppOperation, jobId: Int) = with(ippClient) {
        exchangeSuccessful(printerUri, ippJobRequest(operation, printerUri, jobId))
    }

    // -------
    // Logging
    // -------

    override fun toString() =
            "IppPrinter: name = $name, makeAndModel = $makeAndModel, state = $state, stateReasons = ${stateReasons.joinToString(",")}"

    fun logDetails() {
        println("PRINTER-$name ($makeAndModel), $state (${stateReasons.joinToString(",")})")
        for (attribute in attributes.values) {
            println("  $attribute")
        }
    }

}