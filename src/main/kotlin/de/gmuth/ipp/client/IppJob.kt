package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.client.IppJobState.*
import de.gmuth.ipp.core.*
import de.gmuth.ipp.core.IppOperation.*
import de.gmuth.ipp.core.IppTag.*
import de.gmuth.log.Logging.getLogger
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.lang.Runtime.getRuntime
import java.net.URI

class IppJob(
    val printer: IppPrinter,
    var attributes: IppAttributesGroup,
    subscriptionAttributes: IppAttributesGroup? = null

) {
    companion object {
        val log = getLogger {}
        var defaultDelayMillis: Long = 3000
    }

    var subscription: IppSubscription? = subscriptionAttributes?.let { IppSubscription(printer, it) }
    val ippConfig = printer.ippConfig

    //--------------
    // IppAttributes
    //--------------

    val id: Int
        get() = attributes.getValue("job-id")

    val uri: URI
        get() = attributes.getValue("job-uri")

    val printerUri: URI
        get() = attributes.getValue("job-printer-uri")

    val state: IppJobState
        get() = IppJobState.fromInt(attributes.getValue("job-state"))

    val stateReasons: List<String>
        get() = attributes.getValues("job-state-reasons")

    val name: IppString
        get() = attributes.getValue("job-name")

    val originatingUserName: IppString
        @SuppressWarnings("kotlin:S1192")
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

    // only supported by Apple CUPS
    val applePrintJobInfo: ApplePrintJobInfo
        get() = ApplePrintJobInfo(attributes)

    fun hasStateReasons() = attributes.containsKey("job-state-reasons")

    fun isPending() = state == Pending
    fun isAborted() = state == Aborted
    fun isCanceled() = state == Canceled
    fun isProcessing() = state == Processing
    fun isProcessingStopped() = state == ProcessingStopped
    fun isTerminated() = state.isTerminated()

    fun isProcessingToStopPoint() =
        hasStateReasons() && stateReasons.contains("processing-to-stop-point")

    fun resourcesAreNotReady() =
        hasStateReasons() && stateReasons.contains("resources-are-not-ready")

    fun getOriginatingUserNameOrAppleJobOwnerOrNull() = when {
        attributes.containsKey("job-originating-user-name") -> originatingUserName.text
        attributes.containsKey("com.apple.print.JobInfo.PMJobOwner") -> applePrintJobInfo.jobOwner
        else -> null
    }

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
                printer.updatePrinterStateAttributes()
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
        if (isCanceled()) log.warn { "job #$id is already 'canceled'" }
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

    @JvmOverloads
    fun sendDocument(
        file: File,
        lastDocument: Boolean = true,
        documentName: String? = null,
        documentNaturalLanguage: String? = null
    ) = sendDocument(FileInputStream(file), lastDocument, documentName, documentNaturalLanguage)

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
            documentName?.let { attribute("document-name", NameWithoutLanguage, it) }
            documentNaturalLanguage?.let { attribute("document-natural-language", NaturalLanguage, it) }
        }
    }

    //------------------------
    // Create-Job-Subscription
    //------------------------

    fun createJobSubscription(notifyEvents: List<String>? = null): IppSubscription {
        val request = ippRequest(CreateJobSubscriptions).apply {
            printer.checkNotifyEvents(notifyEvents)
            createSubscriptionAttributesGroup(notifyEvents, notifyJobId = id)
        }
        val subscriptionAttributes = exchange(request).getSingleAttributesGroup(Subscription)
        return IppSubscription(printer, subscriptionAttributes).apply {
            subscription = this
            if (notifyEvents != null && !events.containsAll(notifyEvents)) {
                log.warn { "server ignored some notifyEvents $notifyEvents, subscribed events: $events" }
            }
        }
    }

    //-------------------------------------------------------------------------------------
    // Cups-Get-Document
    //
    // * Apple CUPS
    // https://www.cups.org/doc/spec-ipp.html#CUPS_GET_DOCUMENT
    // CVE-2023-32360, no password is required on MacOS <13.4, <12.6.6, <11.7.7
    //
    // * OpenPrinting CUPS
    // https://openprinting.github.io/cups/doc/spec-ipp.html#CUPS_GET_DOCUMENT
    // Security Advisory
    // https://github.com/OpenPrinting/cups/security/advisories/GHSA-7pv4-hx8c-gr4g
    // no password required for CUPS <2.4.3
    // https://github.com/OpenPrinting/cups/commit/a0c8b9c9556882f00c68b9727a95a1b6d1452913
    //
    // * PreserveJobFiles configuration defaults to one day
    // https://www.cups.org/doc/man-cupsd.conf.html
    // https://openprinting.github.io/cups/doc/man-cupsd.conf.html
    //-------------------------------------------------------------------------------------

    fun cupsGetDocument(documentNumber: Int = 1): IppDocument {
        log.debug { "cupsGetDocument #$documentNumber for job #$id" }
        if (documentNumber > numberOfDocuments) log.warn { "job has only $numberOfDocuments document(s)" }
        val request = ippRequest(CupsGetDocument).apply {
            operationGroup.attribute("document-number", Integer, documentNumber)
        }
        return IppDocument(this, exchange(request))
    }

    fun cupsGetDocuments() =
        (1..numberOfDocuments).map { cupsGetDocument(it) }

    fun cupsGetAndSaveDocuments(
        directory: File = printerDirectory(),
        overwrite: Boolean = true,
        command: String? = null,
        onIppExceptionThrow: Boolean = true
    ): Collection<File> =
        try {
            cupsGetDocuments()
                .map { document -> document.save(directory, overwrite = overwrite) }
                .onEach { file -> command?.run { getRuntime().exec(arrayOf(command, file.absolutePath)) } }
        } catch (ippException: IppException) {
            if (onIppExceptionThrow) {
                throw ippException
            } else {
                log.error { "Failed to get and save documents for job #$id: ${ippException.message}" }
                emptyList()
            }
        }

    //-----------------------
    // Delegate to IppPrinter
    //-----------------------

    private fun printerDirectory() =
        printer.printerDirectory(printerUri.toString().substringAfterLast("/"))

    fun ippRequest(operation: IppOperation, requestedAttributes: List<String>? = null) =
        printer.ippRequest(
            operation, id, requestedAttributes,
            userName = when {
                ippConfig.ippJobUseJobOwnerAsUserName && attributes.containsKey("com.apple.print.JobInfo.PMJobOwner") -> applePrintJobInfo.jobOwner
                ippConfig.ippJobUseJobOwnerAsUserName && attributes.containsKey("job-originating-user-name") -> originatingUserName.text
                else -> ippConfig.userName
            }
        )

    fun exchange(request: IppRequest) =
        printer.exchange(request)

    // -------
    // Logging
    // -------

    @SuppressWarnings("kotlin:S3776")
    override fun toString(): String = with(attributes) {
        StringBuffer().run {
            append("Job #$id:")
            if (containsKey("job-state")) append(" state=$state")
            if (hasStateReasons()) append(" (reasons=${stateReasons.joinToString(",")})")
            if (containsKey("job-name")) append(", name=$name")
            if (containsKey("job-impressions-completed")) append(", impressions-completed=$impressionsCompleted")
            if (containsKey("job-originating-user-name")) append(", originating-user-name=$originatingUserName")
            if (containsKey("com.apple.print.JobInfo.PMJobName")) append(", $applePrintJobInfo")
            if (containsKey("number-of-documents")) append(", number-of-documents=$numberOfDocuments")
            if (containsKey("job-printer-uri")) append(", job-printer-uri=$printerUri")
            toString()
        }
    }

    fun logDetails() = attributes.logDetails(title = "JOB-$id")
}