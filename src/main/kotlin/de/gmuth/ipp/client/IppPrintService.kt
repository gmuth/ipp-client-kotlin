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
    var verbose: Boolean = true
    var httpAuth: Http.Auth? = null
    val ippPrinter: IppPrinter

    init {
        //ippClient.verbose = true
        val response = ippClient.getPrinterAttributes(printerUri)
        ippPrinter = IppPrinter(response.printerGroup)
        ippPrinter.logDetails()
    }

    // ===== PRINT =====

    fun printFile(
            file: File,
            vararg jobParameters: IppJobParameter,
            documentFormat: String = "application/octet-stream",
            waitForTermination: Boolean = false

    ): IppJob {

        val request = ippClient.ippRequest(IppOperation.PrintJob).apply {
            operationGroup.attribute("printer-uri", IppTag.Uri, printerUri)
            operationGroup.attribute("document-format", IppTag.MimeMediaType, documentFormat)
            operationGroup.attribute("job-name", IppTag.NameWithoutLanguage, file.name)
            with(ippAttributesGroup(IppTag.Job)) {
                jobParameters.forEach { jobParameter -> put(jobParameter.toIppAttribute(ippPrinter)) }
            }
        }
        if (verbose && !ippClient.verbose) request.logDetails("IPP: ")

        val response = ippClient.exchangeSuccessful(printerUri, request, documentInputStream = FileInputStream(file))
        val job = IppJob(response.jobGroup)

        if (waitForTermination) {
            waitForTermination(job)
            //job.waitForTermination()
            if (verbose) job.logDetails()
        } else {
            if (verbose) println(job)
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

        val request = ippClient.ippRequest(IppOperation.PrintUri).apply {
            operationGroup.attribute("printer-uri", IppTag.Uri, printerUri)
            operationGroup.attribute("document-uri", IppTag.Uri, documentUri)
            operationGroup.attribute("document-format", IppTag.MimeMediaType, documentFormat)
            operationGroup.attribute("job-name", IppTag.NameWithoutLanguage, documentUri.path)
            with(ippAttributesGroup(IppTag.Job)) {
                jobParameters.forEach { jobParameter -> put(jobParameter.toIppAttribute(ippPrinter)) }
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
                jobParameters.forEach { jobParameter -> put(jobParameter.toIppAttribute(ippPrinter)) }
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

    fun refreshJobAttributes(job: IppJob) {
        val response = ippClient.getJobAttributes(job.uri)
        //job.readFrom(response.jobGroup)
        job.attributes = response.jobGroup
    }

    fun refreshJobAttributes(jobs: List<IppJob>) {
        for (job in jobs) {
            refreshJobAttributes(job)
        }
    }

    fun waitForTermination(
            job: IppJob,
            refreshRate: Duration = Duration.ofSeconds(1),
            refreshPrinterAttributes: Boolean = false
    ) {
        do {
            Thread.sleep(refreshRate.toMillis())
            if (refreshPrinterAttributes) {
                refreshPrinterAttributes()
            }
            refreshJobAttributes(job)

            if (verbose) {
                if (refreshPrinterAttributes) {
                    println("printer-state = ${ippPrinter.printerState}, job-state = ${job.state}")
                } else {
                    println("job-state = ${job.state}, job-impressions-completed = ${job.impressionsCompleted}")
                }
            }

        } while (job.state?.isNotTerminated()!!)
        if (verbose) job.logDetails()
    }

    fun cancelJob(jobId: Int) = ippClient.cancelJob(printerUri, jobId)

    fun cancelJob(job: IppJob) = ippClient.cancelJob(job.uri)

    // ===== PRINTER METHODS =====

    fun refreshPrinterAttributes() {
        val response = ippClient.getPrinterAttributes(printerUri, IppPrinter.requestAttributes)
        ippPrinter.readFrom(response.printerGroup)
    }

    // identify-actions-supported (1setOf keyword) = flash,sound,display,speak
    fun identifyPrinter(action: String = "sound") {
        val request = ippClient.ippRequest(IppOperation.IdentifyPrinter).apply {
            operationGroup.attribute("printer-uri", IppTag.Uri, printerUri)
            operationGroup.attribute("identify-actions", IppTag.Keyword, action)
        }
        ippClient.exchangeSuccessful(printerUri, request, httpAuth = httpAuth)
    }

    fun pausePrinter() = ippClient.pausePrinter(printerUri, httpAuth)

    fun resumePrinter() = ippClient.resumePrinter(printerUri, httpAuth)

}