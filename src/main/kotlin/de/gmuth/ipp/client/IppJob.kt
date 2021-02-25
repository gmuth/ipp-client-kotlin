package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.*
import de.gmuth.log.Logging
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.InputStream
import java.net.URI

class IppJob(
        val printer: IppPrinter,
        var attributes: IppAttributesGroup
) {
    companion object {
        val log = Logging.getLogger {}
        var defaultDelayMillis: Long = 3000
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

    @JvmOverloads
    fun getJobAttributes(requestedAttributes: List<String>? = null): IppResponse {
        val request = ippRequest(IppOperation.GetJobAttributes, requestedAttributes)
        return exchangeSuccessful(request)
    }

    fun updateAllAttributes() {
        attributes = getJobAttributes().jobGroup
    }

    //------------------------------------------
    // Wait for terminal state (RFC 8011 5.3.7.)
    //------------------------------------------

    @JvmOverloads
    fun waitForTermination(delayMillis: Long = defaultDelayMillis) {
        log.info { "wait for termination of job #$id" }
        var lastToString = toString()
        do {
            runBlocking { delay(delayMillis) }
            updateAllAttributes()
            // state, stateReason or impressionsCompleted changed?
            if (toString() != lastToString) {
                lastToString = toString()
                log.info { lastToString }
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

    @JvmOverloads
    fun sendDocument(inputStream: InputStream, lastDocument: Boolean = true) {
        val request = ippRequest(IppOperation.SendDocument).apply {
            operationGroup.attribute("last-document", IppTag.Boolean, lastDocument)
            documentInputStream = inputStream
        }
        val response = exchangeSuccessful(request)
        attributes = response.jobGroup
    }

    //-----------------------
    // delegate to IppPrinter
    //-----------------------

    fun ippRequest(operation: IppOperation, requestedAttributes: List<String>? = null) =
            printer.ippRequest(operation, id, requestedAttributes)

    fun exchangeSuccessfulIppRequest(operation: IppOperation) =
            printer.exchangeSuccessfulIppRequest(operation, id)

    fun exchangeSuccessful(request: IppRequest) =
            printer.exchangeSuccessful(request)

    // -------
    // Logging
    // -------

    override fun toString(): String =
            // workaround: operation is used by waitForTermination() to detect changes
            StringBuffer("id=$id, uri=$uri").apply {
                // by default Get-Jobs operation only returns job-id and job-uri
                if (attributes.containsKey("job-originating-user-name")) append(", originatingUserName=$originatingUserName")
                if (attributes.containsKey("job-name")) append(", name=$name")
                if (attributes.containsKey("job-state")) append(", state=$state")
                if (attributes.containsKey("job-state-reasons")) append(", stateReasons=$stateReasons")
                if (attributes.containsKey("job-impressions-completed")) append(", impressionsCompleted=$impressionsCompleted")
            }.toString()

    fun logDetails() =
            attributes.logDetails("", "JOB-$id")

}