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
import java.io.InputStream
import java.net.URI

class IppJob(
        val printer: IppPrinter,
        var attributes: IppAttributesGroup
) {
    companion object {
        var defaultRefreshRateMillis: Long = 3000
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

    fun waitForTermination(refreshRateMillis: Long = defaultRefreshRateMillis) {
        println("wait for terminal state of job #$id")
        do {
            runBlocking {
                delay(refreshRateMillis)
            }
            updateAttributes()
            println("job-state=$state, job-impressions-completed=$impressionsCompleted")
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

    fun sendDocument(inputStream: InputStream, lastDocument: Boolean = true) {
        val request = ippJobRequest(IppOperation.SendDocument).apply {
            operationGroup.attribute("last-document", IppTag.Boolean, lastDocument)
            documentInputStream = inputStream
        }
        val response = exchangeSuccessful(request)
        attributes = response.jobGroup
    }

    //-----------------------
    // delegate to IppPrinter
    //-----------------------

    private fun ippJobRequest(operation: IppOperation) =
            printer.ippJobRequest(operation, id)

    private fun exchangeSuccessful(request: IppRequest) =
            printer.exchangeSuccessful(request)

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
                    ", state=$state, stateReasons=$stateReasons"
                }
        return "IppJob: id=$id, uri=$uri$stateString"
    }

    fun logDetails() = attributes.logDetails("", "JOB-$id")

}