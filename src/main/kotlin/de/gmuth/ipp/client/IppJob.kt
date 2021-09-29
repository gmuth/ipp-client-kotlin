package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2021 Gerhard Muth
 */

import de.gmuth.ipp.client.IppJobState.*
import de.gmuth.ipp.core.*
import de.gmuth.ipp.core.IppOperation.*
import de.gmuth.ipp.core.IppTag.*
import de.gmuth.log.Logging
import java.io.InputStream
import java.net.URI

class IppJob(
        val printer: IppPrinter,
        var attributes: IppAttributesGroup,
        subscriptionAttributes: IppAttributesGroup? = null

) {
    companion object {
        val log = Logging.getLogger {}
        var defaultDelayMillis: Long = 3000
    }

    var subscription: IppSubscription? = subscriptionAttributes?.let { IppSubscription(printer, it) }

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

    val numberOfDocuments: Int
        get() = attributes.getValue("number-of-documents")

    val documentNameSupplied: IppString
        get() = attributes.getValue("document-name-supplied")

    fun hasStateReasons() = attributes.containsKey("job-state-reasons")

    fun isProcessing() = state == Processing

    fun isProcessingStopped() = state == ProcessingStopped

    fun isTerminated() = state in listOf(Canceled, Aborted, Completed)

    fun isProcessingToStopPoint() =
            hasStateReasons() && stateReasons.contains("processing-to-stop-point")

    //-------------------
    // Get-Job-Attributes
    //-------------------

    //  RFC 8011 4.3.4 groups: 'all', 'job-template', 'job-description'

    @JvmOverloads
    fun getJobAttributes(requestedAttributes: List<String>? = null) =
            exchange(ippRequest(GetJobAttributes, requestedAttributes))

    fun getJobAttributes(vararg requestedAttribute: String) =
            getJobAttributes(requestedAttribute.toList())

    fun updateAttributes(jobAttributeGroupName: String = "all") {
        attributes = getJobAttributes(jobAttributeGroupName).jobGroup
    }

    //------------------------------------------
    // Wait for terminal state (RFC 8011 5.3.7.)
    //------------------------------------------

    @JvmOverloads
    fun waitForTermination(delayMillis: Long = defaultDelayMillis) {
        log.info { "wait for termination of job #$id" }
        var lastPrinterString = ""
        var lastJobString = toString()
        log.info { lastJobString }
        while (!isTerminated()) {
            Thread.sleep(delayMillis)
            updateAttributes()
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

    fun hold() = exchange(ippRequest(HoldJob)).also { updateAttributes() }

    fun release() = exchange(ippRequest(ReleaseJob)).also { updateAttributes() }

    fun restart() = exchange(ippRequest(RestartJob)).also { updateAttributes() }

    fun cancel(messageForOperator: String? = null): IppResponse { // RFC 8011 4.3.3
        if (isProcessingToStopPoint()) log.warn { "job #$id is already 'processing-to-stop-point'" }
        val request = ippRequest(CancelJob).apply {
            messageForOperator?.let { operationGroup.attribute("message", TextWithoutLanguage, it.toIppString()) }
        }
        log.info { "cancel job#$id" }
        return exchange(request).also { updateAttributes() }
    }

    //--------------
    // Send-Document
    //--------------

    @JvmOverloads
    fun sendDocument(
            inputStream: InputStream,
            lastDocument: Boolean = true,
            documentName: String? = null,
            documentNaturalLanguage: String? = null
    ) {
        val request = documentRequest(SendDocument, lastDocument, documentName, documentNaturalLanguage).apply {
            documentInputStream = inputStream
        }
        attributes = exchange(request).jobGroup
    }

    //---------
    // Send-URI
    //---------

    @JvmOverloads
    fun sendUri(
            documentUri: URI,
            lastDocument: Boolean = true,
            documentName: String? = null,
            documentNaturalLanguage: String? = null
    ) {
        val request = documentRequest(SendURI, lastDocument, documentName, documentNaturalLanguage).apply {
            operationGroup.attribute("document-uri", Uri, documentUri)
        }
        attributes = exchange(request).jobGroup
    }

    protected fun documentRequest(
            operation: IppOperation,
            lastDocument: Boolean,
            documentName: String?,
            documentNaturalLanguage: String?
    ) = ippRequest(operation).apply {
        operationGroup.run {
            attribute("last-document", IppTag.Boolean, lastDocument)
            documentName?.let { attribute("document-name", NameWithoutLanguage, it.toIppString()) }
            documentNaturalLanguage?.let { attribute("document-natural-language", NaturalLanguage, it) }
        }
    }

    //------------------------
    // Create-Job-Subscription
    //------------------------

    fun createJobSubscription(notifyEvents: List<String>? = null): IppSubscription {
        val request = ippRequest(CreateJobSubscriptions).apply {
            printer.createSubscriptionGroup(this, notifyEvents, notifyJobId = id)
        }
        val subscriptionAttributes = exchange(request).getSingleAttributesGroup(Subscription)
        return IppSubscription(printer, subscriptionAttributes).apply {
            subscription = this
            if (notifyEvents != null && !events.containsAll(notifyEvents)) {
                log.warn { "server ignored some notifyEvents $notifyEvents, subscribed events: $events" }
            }
        }
    }

    //---------------------------------------------------------
    // Cups-Get-Document
    // https://www.cups.org/doc/spec-ipp.html#CUPS_GET_DOCUMENT
    //---------------------------------------------------------

    fun cupsGetDocument(documentNumber: Int = 1): IppDocument {
        if (!printer.isCups()) log.warn { "printer is not CUPS: ${printer.printerUri}" }
        if (attributes.containsKey("number-of-documents") && documentNumber > numberOfDocuments) {
            log.warn { "job has only $numberOfDocuments document(s)" }
        }
        val request = ippRequest(CupsGetDocument).apply {
            operationGroup.attribute("document-number", Integer, documentNumber)
        }
        return IppDocument(this, exchange(request))
    }

    //-----------------------
    // delegate to IppPrinter
    //-----------------------

    fun ippRequest(operation: IppOperation, requestedAttributes: List<String>? = null) =
            printer.ippRequest(operation, id, requestedAttributes)

    fun exchange(request: IppRequest) =
            printer.exchange(request)

    // -------
    // Logging
    // -------

    override fun toString(): String = with(attributes) {
        StringBuffer().run {
            append("Job #$id:")
            if (containsKey("job-state")) append(" state=$state")
            if (hasStateReasons()) append(" (reasons=${stateReasons.joinToString(",")})")
            if (containsKey("job-name")) append(", name=$name")
            if (containsKey("job-originating-user-name")) append(", originating-user-name=$originatingUserName")
            if (containsKey("job-impressions-completed")) append(", impressions-completed=$impressionsCompleted")
            toString()
        }
    }

    fun logDetails() = attributes.logDetails(title = "JOB-$id")
}