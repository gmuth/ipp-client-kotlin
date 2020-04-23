package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.http.Http
import de.gmuth.ipp.core.IppOperation
import de.gmuth.ipp.core.IppResponse
import de.gmuth.ipp.core.IppTag
import java.io.File
import java.io.FileInputStream
import java.net.URI
import java.time.Duration

class IppPrintService(private val printerUri: URI) {

    private val ippClient = IppClient()
    var verbose: Boolean = false
    var httpAuth: Http.Auth? = null
    val printer: IppPrinter

    init {
        //ippClient.verbose = true
        println("IppPrintService for $printerUri")
        val response = ippClient.getPrinterAttributes(printerUri)
        printer = IppPrinter(response.printerGroup)
        if (verbose) println(printer)
    }

    var requestingUserName: String?
        get() = ippClient.requestingUserName
        set(value) {
            ippClient.requestingUserName = value
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
        val response = ippClient.exchangeSuccessful(
                printerUri, request, documentInputStream = FileInputStream(file), httpAuth = httpAuth
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
        val response = ippClient.exchangeSuccessful(
                printerUri, request, "Print-Uri $documentUri", httpAuth = httpAuth
        )
        return waitForJobTermination(response, waitForTermination)
    }

    // ---- factory method for IppRequest with Operation Print-Job or Print-Uri

    private fun printRequest(
            printOperation: IppOperation,
            documentFormat: String,
            jobName: String,
            jobParameters: List<IppJobParameter>

    ) = ippClient.ippRequest(printOperation, printerUri).apply {
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

    //------------
    // JOB METHODS
    //------------

    // which-jobs-supported (1setOf keyword) = completed,not-completed,aborted,all,canceled,pending,pending-held,processing,processing-stopped
    fun getJobs(whichJobs: String? = null) = ippClient
            .getJobs(printerUri, whichJobs)
            .getAttributesGroups(IppTag.Job)
            .map { jobGroup -> IppJob(jobGroup) }

    fun getJob(jobId: Int) =
            IppJob(ippClient.getJobAttributes(printerUri, jobId).jobGroup)

    fun updateJobAttributes(job: IppJob) {
        if (printerUri.scheme != job.uri.scheme) {
            println("WARN: printerUri.scheme = ${printerUri.scheme}, jobUri.scheme = ${job.uri.scheme}")
        }
        job.attributes = ippClient.getJobAttributes(printerUri, job.id).jobGroup
    }

    fun updateJobAttributes(jobs: List<IppJob>) =
            jobs.stream().forEach { updateJobAttributes(it) }

    fun waitForTermination(job: IppJob, refreshRate: Duration = Duration.ofMillis(888)) {
        println("wait for termination of job #${job.id}")
        do {
            Thread.sleep(refreshRate.toMillis())
            updateJobAttributes(job)
            println("job-state = ${job.state}, job-impressions-completed = ${job.impressionsCompleted}")
        } while (!job.isTerminated())
    }

    fun cancelJob(jobId: Int) = ippClient.cancelJob(printerUri, jobId)

    fun cancelJob(job: IppJob) = cancelJob(job.id)

    //----------------------
    // PRINTER ADMIN METHODS
    //----------------------

    // identify-actions-supported (1setOf keyword) = flash,sound,display,speak
    fun identifyPrinter(action: String) = ippClient.identifyPrinter(printerUri, action, httpAuth)

    fun pausePrinter() = ippClient.pausePrinter(printerUri, httpAuth)

    fun resumePrinter() = ippClient.resumePrinter(printerUri, httpAuth)

}