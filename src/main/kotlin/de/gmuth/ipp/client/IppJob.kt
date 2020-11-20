package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppAttributesGroup
import de.gmuth.ipp.core.IppOperation
import de.gmuth.ipp.core.IppString
import de.gmuth.ipp.core.IppTag
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.InputStream
import java.net.URI

class IppJob(
        val printer: IppPrinter,
        var attributes: IppAttributesGroup
) {
    var verbose: Boolean = true

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

    val state: IppJobState
        get() = IppJobState.fromInt(attributes.getValue("job-state"))

    val stateReasons: List<String>
        get() = attributes.getValues("job-state-reasons")

    val name: IppString
        get() = attributes.getValue("job-name")

    val originatingUserName: IppString
        get() = attributes.getValue("job-originating-user-name")

    val kOctets: Int
        get() = attributes.getValue("job-k-octets")

    val impressionsCompleted: Int
        get() = attributes.getValue("job-impressions-completed")

    val mediaSheetsCompleted: Int
        get() = attributes.getValue("job-media-sheets-completed")

    fun isTerminated() =
            state.isTerminated()

    //-------------------
    // Get-Job-Attributes
    //-------------------

    fun getJobAttributes() =
            exchangeSuccessfulIppRequest(IppOperation.GetJobAttributes)

    fun updateAllAttributes() {
        attributes = getJobAttributes().jobGroup
    }

    //------------------------------------------
    // Wait for terminal state (RFC 8011 5.3.7.)
    //------------------------------------------

    fun waitForTermination(refreshRateMillis: Long = defaultRefreshRateMillis) {
        if (verbose) println("wait for terminal state of job #$id")
        do {
            runBlocking { delay(refreshRateMillis) }
            updateAllAttributes()
            if (verbose) {
                println(StringBuffer("job-id=$id, job-state=$state").apply {
                    if (attributes.available("job-impressions-completed")) append(", job-impressions-completed=$impressionsCompleted")
                })
            }
        } while (!isTerminated())
    }

    //-----------
    // Cancel-Job
    //-----------

    fun cancel() =
            exchangeSuccessfulIppRequest(IppOperation.CancelJob)

    //---------
    // Hold-Job
    //---------

    fun hold() =
            exchangeSuccessfulIppRequest(IppOperation.HoldJob)

    //------------
    // Release-Job
    //-------------

    fun release() =
            exchangeSuccessfulIppRequest(IppOperation.ReleaseJob)

    //--------------
    // Send-Document
    //--------------

    fun sendDocument(inputStream: InputStream, lastDocument: Boolean = true) {
        val request = ippJobRequest(IppOperation.SendDocument).apply {
            operationGroup.attribute("last-document", IppTag.Boolean, lastDocument)
            documentInputStream = inputStream
        }
        val response = printer.exchangeSuccessful(request)
        attributes = response.jobGroup
    }

    //-----------------------
    // delegate to IppPrinter
    //-----------------------

    private fun ippJobRequest(operation: IppOperation) =
            printer.ippRequest(operation, id)

    private fun exchangeSuccessfulIppRequest(operation: IppOperation) =
            printer.exchangeSuccessfulIppRequest(operation, id)

    // -------
    // Logging
    // -------

    override fun toString(): String =
            StringBuffer("IppJob: id=$id, uri=$uri").apply {
                // by default Get-Jobs operation only returns job-id and job-uri
                if (attributes.available("job-state")) append(", state=$state")
                if (attributes.available("job-state-reasons")) append(", stateReasons=$stateReasons")
                if (attributes.available("job-originating-user-name")) append(", originatingUserName=$originatingUserName")
                if (attributes.available("job-name")) append(", name=$name")
            }.toString()

    fun logDetails() =
            attributes.logDetails("", "JOB-$id")

}