package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppJob
import de.gmuth.ipp.core.toIppJob
import java.io.File
import java.net.URI

class IppPrintService(private val printerUri: URI) {

    private val ippClient = IppClient()

    fun printFile(file: File, vararg jobParameters: IppJobParameter, waitForTermination: Boolean = false) =
            printFile(file, jobParameters.toList(), waitForTermination)

    fun printFile(file: File, jobParameters: List<IppJobParameter>, waitForTermination: Boolean = false) {

        val printJob = IppPrintJob(printerUri, file = file, jobParameters = jobParameters)
        printJob.logDetails("IPP: ")

        val job = sendPrintJob(printJob, waitForTermination)
        job.logDetails()
    }

    fun sendPrintJob(printJob: IppPrintJob, waitForTermination: Boolean = false): IppJob {
        val response = with(printJob) {
            ippClient.exchangeSuccessful(printerUri, this, documentInputStream = documentInputStream)
        }
        val job = response.jobGroup.toIppJob()
        if (waitForTermination) {
            ippClient.waitForTermination(job)
        }
        return job
    }

}