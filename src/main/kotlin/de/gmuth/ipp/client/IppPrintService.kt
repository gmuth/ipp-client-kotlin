package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.http.Http
import de.gmuth.ipp.core.*
import java.io.File
import java.io.FileInputStream
import java.net.URI
import java.time.Duration
import kotlin.streams.toList

class IppPrintService(private val printerUri: URI) {

    val ippClient = IppClient()
    var verbose: Boolean = true
    var httpAuth: Http.Auth? = null

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
            jobGroup.attribute("job-name", IppTag.NameWithoutLanguage, jobName)
            jobParameters.forEach { jobParameter -> jobGroup.put(jobParameter.toIppAttribute()) }
        }
        if (verbose) request.logDetails("IPP: ")
        val response = ippClient.exchangeSuccessful(printerUri, request, documentInputStream = FileInputStream(file))
        val job = response.jobGroup.toIppJob()

        if (waitForTermination) waitForTermination(job)
        if (verbose) job.logDetails()
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
            jobGroup.attribute("job-name", IppTag.NameWithoutLanguage, jobName)
            jobParameters.forEach { jobParameter -> jobGroup.put(jobParameter.toIppAttribute()) }
        }
        if (verbose) request.logDetails("IPP: ")

        val response = ippClient.exchangeSuccessful(printerUri, request, "Print-Uri $documentUri")
        val job = response.jobGroup.toIppJob()

        if (waitForTermination) waitForTermination(job)
        if (verbose) job.logDetails()
        return job
    }

    // ============================================================================================================================ JOB HANDLING

    fun getJob(jobId: Int) = ippClient.getJobAttributes(printerUri, jobId).jobGroup.toIppJob()

    fun getJobs(): List<IppJob> {
        val request = IppRequest(IppOperation.GetJobs, printerUri)
        val response = ippClient.exchangeSuccessful(printerUri, request)
        return response.getAttributesGroups(IppTag.Job).stream()
                .map { jobGroup -> jobGroup.toIppJob() }.toList()
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
        println("canceled: $job")
    }

    // ============================================================================================================================ PRINTER HANDLING

    fun pausePrinter() = sendPrinterOperation(printerUri, IppOperation.PausePrinter)

    fun resumePrinter() = sendPrinterOperation(printerUri, IppOperation.ResumePrinter)

    private fun sendPrinterOperation(printerUri: URI, printerOperation: IppOperation) {
        val request = IppRequest(printerOperation, printerUri)
        ippClient.exchangeSuccessful(printerUri, request, httpAuth = httpAuth)
    }

}