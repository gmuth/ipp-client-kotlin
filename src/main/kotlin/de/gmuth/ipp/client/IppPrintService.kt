package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.http.Http
import de.gmuth.ipp.core.IppOperation
import de.gmuth.ipp.core.IppRequest
import de.gmuth.ipp.core.IppTag
import java.io.File
import java.io.FileInputStream
import java.net.URI
import java.time.Duration
import kotlin.streams.toList

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

    // ============================================================================================================================ PRINT

    fun printFile(
            file: File,
            vararg jobParameters: IppJobParameter,
            documentFormat: String = "application/octet-stream",
            jobName: String = file.name,
            waitForTermination: Boolean = false

    ): IppJob {

        val request = IppRequest(IppOperation.PrintJob, printerUri).apply {
            operationGroup.attribute("document-format", IppTag.MimeMediaType, documentFormat)
            with(newJobGroup()) {
                attribute("job-name", IppTag.NameWithoutLanguage, jobName)
                jobParameters.forEach { jobParameter -> put(jobParameter.toIppAttribute(ippPrinter)) }
            }
        }
        if (verbose && !ippClient.verbose) request.logDetails("IPP: ")

        val response = ippClient.exchangeSuccessful(printerUri, request, documentInputStream = FileInputStream(file))
        val job = IppJob(response.jobGroup)

        if (waitForTermination) {
            waitForTermination(job)
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
            jobName: String = documentUri.path,
            waitForTermination: Boolean = false

    ): IppJob {

        val request = IppRequest(IppOperation.PrintUri, printerUri).apply {
            operationGroup.attribute("document-uri", IppTag.Uri, documentUri)
            operationGroup.attribute("document-format", IppTag.MimeMediaType, documentFormat)
            with(newJobGroup()) {
                attribute("job-name", IppTag.NameWithoutLanguage, jobName)
                jobParameters.forEach { jobParameter -> put(jobParameter.toIppAttribute(ippPrinter)) }
            }
        }
        if (verbose) request.logDetails("IPP: ")

        val response = ippClient.exchangeSuccessful(printerUri, request, "Print-Uri $documentUri")
        val job = IppJob(response.jobGroup)

        if (waitForTermination) waitForTermination(job)
        if (verbose) job.logDetails()
        return job
    }

    // ============================================================================================================================ JOB HANDLING

    fun getJob(jobId: Int): IppJob {
        return IppJob(ippClient.getJobAttributes(printerUri, jobId).jobGroup)
    }

    fun getJobs(): List<IppJob> {
        val request = IppRequest(IppOperation.GetJobs, printerUri)
        val response = ippClient.exchangeSuccessful(printerUri, request)
        return response.getAttributesGroups(IppTag.Job).stream()
                .map { jobGroup -> IppJob(jobGroup) }.toList()
    }

    fun refreshJob(job: IppJob) {
        val response = ippClient.getJobAttributes(job.uri)
        job.readFrom(response.jobGroup)
    }

    fun refreshJobs(jobs: List<IppJob>) = jobs.stream().forEach { job -> refreshJob(job) }

    fun waitForTermination(job: IppJob, refreshRate: Duration = Duration.ofSeconds(1)) {
        do {
            Thread.sleep(refreshRate.toMillis())
            refreshJob(job)
            if (verbose) println("job-state = ${job.state}")
        } while (!job.state?.isTerminated()!!)
        if (verbose) job.logDetails()
    }

    fun cancelJob(jobId: Int) {
        val request = IppRequest(IppOperation.CancelJob, printerUri).apply {
            operationGroup.attribute("job-id", IppTag.Integer, jobId)
        }
        ippClient.exchangeSuccessful(printerUri, request)
        println("canceled: job #$jobId")
    }

    fun cancelJob(job: IppJob) {
        val request = IppRequest(IppOperation.CancelJob).apply {
            operationGroup.attribute("job-uri", IppTag.Uri, job.uri)
        }
        ippClient.exchangeSuccessful(job.uri, request)
        println("canceled: ${job.uri}")
    }

    // ============================================================================================================================ PRINTER HANDLING

    fun refreshPrinter(printer: IppPrinter) {
        val response = ippClient.getPrinterAttributes(printerUri, IppPrinter.requestAttributes)
        printer.readFrom(response.printerGroup)
    }

    fun pausePrinter() = sendPrinterOperation(printerUri, IppOperation.PausePrinter)

    fun resumePrinter() = sendPrinterOperation(printerUri, IppOperation.ResumePrinter)

    private fun sendPrinterOperation(printerUri: URI, printerOperation: IppOperation) {
        val request = IppRequest(printerOperation, printerUri)
        ippClient.exchangeSuccessful(printerUri, request, httpAuth = httpAuth)
    }

}