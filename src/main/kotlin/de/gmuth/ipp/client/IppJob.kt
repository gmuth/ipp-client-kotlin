package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2021 Gerhard Muth
 */

import de.gmuth.ipp.client.IppJobState.*
import de.gmuth.ipp.core.*
import de.gmuth.ipp.core.IppOperation.*
import de.gmuth.log.Logging
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

    val impressionsCompleted: Int
        get() = attributes.getValue("job-impressions-completed")

    val mediaSheetsCompleted: Int
        get() = attributes.getValue("job-media-sheets-completed")

    val kOctets: Int
        get() = attributes.getValue("job-k-octets")

    fun isProcessing() = state == Processing
    fun isProcessingStopped() = state == ProcessingStopped
    fun isTerminated() = state in listOf(Canceled, Aborted, Completed)

    //-------------------
    // Get-Job-Attributes
    //-------------------

    @JvmOverloads
    fun getJobAttributes(requestedAttributes: List<String>? = null) =
            exchangeSuccessfulIppRequest(GetJobAttributes, requestedAttributes)

    fun updateAllAttributes() {
        attributes = getJobAttributes().jobGroup
    }

    //------------------------------------------
    // Wait for terminal state (RFC 8011 5.3.7.)
    //------------------------------------------

    @JvmOverloads
    fun waitForTermination(delayMillis: Long = defaultDelayMillis) {
        log.info { "wait for termination of job #$id" }
        var lastJobString = ""
        var lastPrinterString = ""
        log.info { toString() }
        while (!isTerminated()) {
            Thread.sleep(delayMillis)
            updateAllAttributes()
            if (toString() != lastJobString) {
                lastJobString = toString()
                log.info { lastJobString }
            }
            if (isProcessingStopped() || lastPrinterString.isNotEmpty()) {
                printer.updateAllAttributes()
                if (printer.toString() != lastPrinterString) {
                    lastPrinterString = printer.toString()
                    log.info { lastPrinterString }
                }
            }
            if (isProcessing() && lastPrinterString.isNotEmpty()) lastPrinterString = ""
        }
    }

    //-------------------
    // Job administration
    //-------------------

    fun hold() = exchangeSuccessfulIppRequest(HoldJob)
    fun cancel() = exchangeSuccessfulIppRequest(CancelJob)
    fun release() = exchangeSuccessfulIppRequest(ReleaseJob)

    //--------------
    // Send-Document
    //--------------

    @JvmOverloads
    fun sendDocument(inputStream: InputStream, lastDocument: Boolean = true) {
        val request = ippRequest(SendDocument).apply {
            operationGroup.attribute("last-document", IppTag.Boolean, lastDocument)
            documentInputStream = inputStream
        }
        attributes = exchangeSuccessful(request).jobGroup
    }

    //-----------------------
    // delegate to IppPrinter
    //-----------------------

    fun ippRequest(operation: IppOperation, requestedAttributes: List<String>? = null) =
            printer.ippRequest(operation, id, requestedAttributes)

    fun exchangeSuccessful(request: IppRequest) =
            printer.exchangeSuccessful(request)

    fun exchangeSuccessfulIppRequest(operation: IppOperation, requestedAttributes: List<String>? = null) =
            printer.exchangeSuccessfulIppRequest(operation, id, requestedAttributes)

    // -------
    // Logging
    // -------

    override fun toString(): String = with(attributes) {
        StringBuffer("id=$id, uri=$uri").apply {
            if (containsKey("job-state")) append(", state=$state")
            if (containsKey("job-state-reasons")) append(", stateReasons=$stateReasons")
            if (containsKey("job-name")) append(", name=$name")
            if (containsKey("job-originating-user-name")) append(", originatingUserName=$originatingUserName")
            if (containsKey("job-impressions-completed")) append(", impressionsCompleted=$impressionsCompleted")
        }.toString()
    }

    fun logDetails() = attributes.logDetails(title = "JOB-$id")

}