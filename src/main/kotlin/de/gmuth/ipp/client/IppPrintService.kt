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

    val ippClient = IppClient()
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

    // ===== PRINT =====

    fun printFile(
            file: File,
            vararg jobParameters: IppJobParameter,
            documentFormat: String = "application/octet-stream",
            waitForTermination: Boolean = false

    ): IppJob {

        val request = ippClient.ippRequest(IppOperation.PrintJob, printerUri).apply {
            operationGroup.attribute("document-format", IppTag.MimeMediaType, documentFormat)
            operationGroup.attribute("job-name", IppTag.NameWithoutLanguage, file.name)
            with(ippAttributesGroup(IppTag.Job)) {
                jobParameters.forEach { jobParameter -> put(jobParameter.toIppAttribute(printer.attributes)) }
            }
        }
        if (verbose && !ippClient.verbose) request.logDetails()

        val response = ippClient.exchangeSuccessful(printerUri, request, documentInputStream = FileInputStream(file))
        val job = IppJob(response.jobGroup)

        if (waitForTermination) {
            waitForTermination(job)
            job.logDetails()
        } else {
            println(job)
        }

        return job
    }

    // -----------------------------------------------
    // RFC 8011 4.2.2: optional operation to print uri
    // -----------------------------------------------

    fun printUri(
            documentUri: URI,
            vararg jobParameters: IppJobParameter,
            documentFormat: String = "application/octet-stream",
            waitForTermination: Boolean = false

    ): IppJob {

        val request = ippClient.ippRequest(IppOperation.PrintUri, printerUri).apply {
            operationGroup.attribute("document-uri", IppTag.Uri, documentUri)
            operationGroup.attribute("document-format", IppTag.MimeMediaType, documentFormat)
            operationGroup.attribute("job-name", IppTag.NameWithoutLanguage, documentUri.path)
            with(ippAttributesGroup(IppTag.Job)) {
                jobParameters.forEach { jobParameter -> put(jobParameter.toIppAttribute(printer.attributes)) }
            }
        }
        if (verbose) request.logDetails("IPP: ")

        val response = ippClient.exchangeSuccessful(printerUri, request, "Print-Uri $documentUri")
        val job = IppJob(response.jobGroup)

        if (waitForTermination) {
            waitForTermination(job)
            //job.waitForTermination()
        }
        if (verbose) job.logDetails()
        return job
    }

    fun validateJob(
            vararg jobParameters: IppJobParameter,
            documentFormat: String = "application/octet-stream"

    ): IppResponse {

        val request = ippClient.ippRequest(IppOperation.ValidateJob).apply {
            operationGroup.attribute("printer-uri", IppTag.Uri, printerUri)
            operationGroup.attribute("document-format", IppTag.MimeMediaType, documentFormat)
            with(ippAttributesGroup(IppTag.Job)) {
                jobParameters.forEach { jobParameter -> put(jobParameter.toIppAttribute(printer.attributes)) }
            }
        }
        if (verbose && !ippClient.verbose) request.logDetails("IPP: ")

        return ippClient.exchangeSuccessful(printerUri, request)
    }

    // ===== JOB METHODS =====

    // which-jobs-supported (1setOf keyword) = completed,not-completed,aborted,all,canceled,pending,pending-held,processing,processing-stopped
    fun getJobs(whichJobs: String? = null): List<IppJob> {
        val response = ippClient.getJobs(printerUri, whichJobs)
        return response
                .getAttributesGroups(IppTag.Job)
                .map { jobGroup -> IppJob(jobGroup) }
    }

    fun getJob(jobId: Int): IppJob {
        val response = ippClient.getJobAttributes(printerUri, jobId)
        return IppJob(response.jobGroup)
    }

    fun updateJobAttributes(job: IppJob) {
        val response = ippClient.getJobAttributes(job.uri)
        job.attributes = response.jobGroup
    }

    fun updateJobAttributes(jobs: List<IppJob>) {
        for (job in jobs) {
            updateJobAttributes(job)
        }
    }

    fun waitForTermination(job: IppJob, refreshRate: Duration = Duration.ofMillis(777)) {
        println("wait for termination of job #${job.id}")
        do {
            Thread.sleep(refreshRate.toMillis())
            updateJobAttributes(job)
            println("job-state = ${job.state}, job-impressions-completed = ${job.impressionsCompleted}")
        } while (!job.isTerminated())
        if(verbose) job.logDetails()
    }

    fun cancelJob(jobId: Int) = ippClient.cancelJob(printerUri, jobId)

    fun cancelJob(job: IppJob) = ippClient.cancelJob(job.uri)

    // ===== PRINTER METHODS =====

    // identify-actions-supported (1setOf keyword) = flash,sound,display,speak
    fun identifyPrinter(action: String = "sound") = ippClient.identifyPrinter(printerUri, action, httpAuth)

    fun pausePrinter() = ippClient.pausePrinter(printerUri, httpAuth)

    fun resumePrinter() = ippClient.resumePrinter(printerUri, httpAuth)

}