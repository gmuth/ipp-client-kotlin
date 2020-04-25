package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppOperation
import de.gmuth.ipp.core.IppResponse
import de.gmuth.ipp.core.IppTag
import java.io.File
import java.io.FileInputStream
import java.net.URI
import java.time.Duration

class IppPrintService(private val printerUri: URI) : IppClient() {

    val printer: IppPrinter

    init {
        println("IppPrintService @ $printerUri")
        val response = getPrinterAttributes()
        printer = IppPrinter(response.printerGroup)
        println(printer)
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
        val response = exchangeSuccessful(
                printerUri, request, documentInputStream = FileInputStream(file)
        )
        return waitForJobTermination(response, waitForTermination)
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
        val response = exchangeSuccessful(
                printerUri, request, "Print-Uri $documentUri"
        )
        return waitForJobTermination(response, waitForTermination)
    }

    // ---- factory method for IppRequest with Operation Print-Job or Print-Uri

    private fun printRequest(
            printOperation: IppOperation,
            documentFormat: String,
            jobName: String,
            jobParameters: List<IppJobParameter>

    ) = ippRequest(printOperation, printerUri).apply {
        operationGroup.attribute("job-name", IppTag.NameWithoutLanguage, jobName)
        operationGroup.attribute("document-format", IppTag.MimeMediaType, documentFormat)
        with(ippAttributesGroup(IppTag.Job)) {
            for (jobParameter in jobParameters) {
                put(jobParameter.toIppAttribute(printer.attributes))
            }
        }
    }

    private fun waitForJobTermination(printResponse: IppResponse, wait: Boolean = false): IppJob {
        val job = IppJob(printResponse.jobGroup)
        if (wait) {
            waitForTermination(job)
        }
        job.logDetails()
        return job
    }

    //===============
    // IppJob methods
    //===============

    // which-jobs-supported (1setOf keyword) = completed,not-completed,aborted,all,canceled,pending,pending-held,processing,processing-stopped
    fun getJobs(whichJobs: String? = null) = getJobsResponse(whichJobs)
            .getAttributesGroups(IppTag.Job)
            .map { jobGroup -> IppJob(jobGroup) }

    fun getJob(jobId: Int) = IppJob(getJobAttributes(jobId).jobGroup)

    fun updateJobAttributes(job: IppJob) {
        if (printerUri.scheme != job.uri.scheme) {
            println("WARN: printerUri.scheme = ${printerUri.scheme}, jobUri.scheme = ${job.uri.scheme}")
        }
        job.attributes = getJobAttributes(job.id).jobGroup
    }

    fun updateJobAttributes(jobs: List<IppJob>) {
        for (job in jobs) {
            updateJobAttributes(job)
        }
    }

    fun waitForTermination(job: IppJob, refreshRate: Duration = Duration.ofMillis(888)) {
        println("wait for termination of job #${job.id}")
        do {
            Thread.sleep(refreshRate.toMillis())
            updateJobAttributes(job)
            println("job-state = ${job.state}, job-impressions-completed = ${job.impressionsCompleted}")
        } while (!job.isTerminated())
    }

    fun cancelJob(job: IppJob) = cancelJob(job.id)

    fun holdJob(job: IppJob) = holdJob(job.id)

    fun releaseJob(job: IppJob) = releaseJob(job.id)

    //===================
    // IppMessage methods
    //===================

    //-----------------------
    // Get-Printer-Attributes
    //-----------------------

    fun getPrinterAttributes(requestedAttributes: List<String> = listOf()): IppResponse {
        val request = ippRequest(IppOperation.GetPrinterAttributes, printerUri).apply {
            if (requestedAttributes.isNotEmpty()) {
                operationGroup.attribute("requested-attributes", IppTag.Keyword, requestedAttributes)
            }
        }
        return exchangeSuccessful(printerUri, request, "Get-Printer-Attributes $printerUri")
    }

    //-------------------
    // Get-Job-Attributes
    //-------------------

    fun getJobAttributes(jobId: Int): IppResponse {
        val request = ippJobRequest(IppOperation.GetJobAttributes, printerUri, jobId)
        return exchangeSuccessful(printerUri, request, "Get-Job-Attributes #$jobId failed")
    }

    //---------
    // Get-Jobs
    //---------

    // which-jobs-supported (1setOf keyword) = completed,not-completed,aborted,all,canceled,pending,pending-held,processing,processing-stopped

    fun getJobsResponse(whichJobs: String? = null): IppResponse {
        val request = ippRequest(IppOperation.GetJobs, printerUri)
        if (whichJobs != null) {
            request.operationGroup.attribute("which-jobs", IppTag.Keyword, whichJobs)
        }
        return exchangeSuccessful(printerUri, request)
    }

    //-----------
    // Cancel-Job
    //-----------

    fun cancelJob(jobId: Int) =
            exchangeSuccessfulIppJobRequest(IppOperation.CancelJob, printerUri, jobId)

    //---------
    // Hold-Job
    //---------

    fun holdJob(jobId: Int) =
            exchangeSuccessfulIppJobRequest(IppOperation.HoldJob, printerUri, jobId)

    //------------
    // Release-Job
    //-------------

    fun releaseJob(jobId: Int) =
            exchangeSuccessfulIppJobRequest(IppOperation.ReleaseJob, printerUri, jobId)

    //-----------------
    // Identify-Printer
    //-----------------

    // identify-actions-supported (1setOf keyword) = flash,sound,display,speak
    //
    fun identifyPrinter(action: String) {
        val request = ippRequest(IppOperation.IdentifyPrinter, printerUri).apply {
            operationGroup.attribute("identify-actions", IppTag.Keyword, action)
        }
        exchangeSuccessful(printerUri, request)
    }

    //--------------
    // Pause-Printer
    //--------------

    fun pausePrinter() =
            exchangeSuccessfulIppRequest(IppOperation.PausePrinter, printerUri)

    //---------------
    // Resume-Printer
    //---------------

    fun resumePrinter() =
            exchangeSuccessfulIppRequest(IppOperation.ResumePrinter, printerUri)

}