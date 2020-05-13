package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppOperation
import de.gmuth.ipp.core.IppRequest
import de.gmuth.ipp.core.IppTag
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URI
import java.time.Duration

class IppJob(
        val printer: IppPrinter,
        var attributes: IppAttributesGroup
) {
    companion object {
        var defaultRefreshRate: Duration = Duration.ofSeconds(3)
    }

    //--------------
    // IppAttributes
    //--------------

    val id: Int
        get() = attributes.getValue("job-id")

    val uri: URI
        get() = attributes.getValue("job-uri")

    val state: IppJobState?
        get() = IppJobState.fromInt(attributes.getValue("job-state") as Int?)

    val stateReasons: List<String>?
        get() = attributes.getValues("job-state-reasons")

    val impressionsCompleted: Int?
        get() = attributes.getValue("job-impressions-completed")

    fun isTerminated() = state != null && state!!.isTerminated()

    //-------------------
    // Get-Job-Attributes
    //-------------------

    private fun getJobAttributes(): IppAttributesGroup {
        val response = exchangeSuccessfulIppJobRequest(IppOperation.GetJobAttributes)
        return response.jobGroup
    }

    fun updateAttributes() {
        attributes = getJobAttributes()
    }

    //------------------------------------------
    // Wait for terminal state (RFC 8011 5.3.7.)
    //------------------------------------------

    fun waitForTermination(refreshRate: Duration = defaultRefreshRate) {
        println("wait for terminal state of job #$id")
        do {
            //Thread.sleep(refreshRate.toMillis())
            runBlocking {
                delay(refreshRate.toMillis())
            }
            updateAttributes()
            println("job-state = $state, job-impressions-completed = $impressionsCompleted")
        } while (!isTerminated())
    }

    //-----------
    // Cancel-Job
    //-----------

    fun cancel() {
        exchangeSuccessfulIppJobRequest(IppOperation.CancelJob)
    }

    //---------
    // Hold-Job
    //---------

    fun hold() {
        exchangeSuccessfulIppJobRequest(IppOperation.HoldJob)
    }

    //------------
    // Release-Job
    //-------------

    fun release() {
        exchangeSuccessfulIppJobRequest(IppOperation.ReleaseJob)
    }

    //--------------
    // Send-Document
    //--------------

    fun sendDocument(file: File, lastDocument: Boolean = true) {
        val request = ippJobRequest(IppOperation.SendDocument).apply {
            operationGroup.attribute("last-document", IppTag.Boolean, lastDocument)
        }
        val response = exchangeSuccessful(request, FileInputStream(file))
        attributes = response.jobGroup
    }

    //-----------------------
    // delegate to IppPrinter
    //-----------------------

    private fun ippJobRequest(operation: IppOperation) =
            printer.ippJobRequest(operation, id)

    private fun exchangeSuccessful(request: IppRequest, documentInputStream: InputStream) =
            printer.exchangeSuccessful(request, documentInputStream)

    private fun exchangeSuccessfulIppJobRequest(operation: IppOperation) =
            printer.exchangeSuccessfulIppJobRequest(operation, id)

    // -------
    // Logging
    // -------

    override fun toString(): String {
        val stateString =
                if (state == null) {
                    ""
                } else {
                    ", state = $state, stateReasons = ${stateReasons?.joinToString(",")}"
                }
        return "IppJob: id = $id, uri = $uri$stateString"
    }

    fun logDetails() = attributes.logDetails("", "JOB-$id")

}