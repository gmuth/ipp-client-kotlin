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

    val attributes = getPrinterAttributes()

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

    fun getPrinterAttributes(requestedAttributes: List<String> = listOf()): IppAttributesGroup {
        val request = ippRequest(IppOperation.GetPrinterAttributes).apply {
            if (requestedAttributes.isNotEmpty()) {
                operationGroup.attribute("requested-attributes", IppTag.Keyword, requestedAttributes)
            }
        }
        return exchangeSuccessful(request).printerGroup
    }

    //----------
    // Print-Job
    //----------

    fun printJob(
            file: File,
            vararg jobParameters: IppJobParameter,
            waitForTermination: Boolean = false

    ): IppJob {
        val request = jobParametersRequest(IppOperation.PrintJob, jobParameters)
        val response = exchangeSuccessful(request, FileInputStream(file))
        return handlePrintResponse(response, waitForTermination)
    }

    //----------
    // Print-Uri
    //----------

    fun printUri(
            documentUri: URI,
            vararg jobParameters: IppJobParameter,
            waitForTermination: Boolean = false

    ): IppJob {
        val request = jobParametersRequest(IppOperation.PrintUri, jobParameters)
        request.operationGroup.attribute("document-uri", IppTag.Uri, documentUri)
        val response = exchangeSuccessful(request)
        return handlePrintResponse(response, waitForTermination)
    }

    //-------------
    // Validate-Job
    //-------------

    fun validateJob(
            vararg jobParameters: IppJobParameter
            //documentFormat: String = "application/octet-stream"

    ): IppResponse {
        val request = jobParametersRequest(IppOperation.ValidateJob, jobParameters)
        return exchangeSuccessful(request)
    }

    //----------
    // Create-Job
    //----------

    fun createJob(
            vararg jobParameters: IppJobParameter

    ): IppJob {
        val request = jobParametersRequest(IppOperation.CreateJob, jobParameters)
        val response = exchangeSuccessful(request)
        return IppJob(this, response.jobGroup)
    }

    // ---- factory method for IppRequest with Operation Print-Job, Print-Uri, Validate-Job, Create-Job

    private fun jobParametersRequest(operation: IppOperation, jobParameters: Array<out IppJobParameter>) =
            ippRequest(operation).apply {
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

    private fun jobParametersContainsName(jobParameters: Array<out IppJobParameter>, value: String) =
            jobParameters.map {
                it.toIppAttribute(attributes)
            }.map {
                it.name
            }.toList().contains(value)

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

    fun ippRequest(operation: IppOperation) =
            ippClient.ippRequest(operation, printerUri)

    fun ippJobRequest(operation: IppOperation, jobId: Int) =
            ippClient.ippJobRequest(operation, printerUri, jobId)

    fun exchangeSuccessful(request: IppRequest, documentInputStream: InputStream? = null) =
            ippClient.exchangeSuccessful(printerUri, request, documentInputStream)

    fun exchangeSuccessfulIppRequest(operation: IppOperation) =
            ippClient.exchangeSuccessful(printerUri, ippRequest(operation))

    fun exchangeSuccessfulIppJobRequest(operation: IppOperation, jobId: Int) =
            ippClient.exchangeSuccessful(printerUri, ippJobRequest(operation, jobId))

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