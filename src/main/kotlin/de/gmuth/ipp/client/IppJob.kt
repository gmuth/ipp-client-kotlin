package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2024 Gerhard Muth
 */

import de.gmuth.ipp.attributes.JobState
import de.gmuth.ipp.attributes.JobState.*
import de.gmuth.ipp.core.*
import de.gmuth.ipp.core.IppOperation.*
import de.gmuth.ipp.core.IppStatus.SuccessfulOk
import de.gmuth.ipp.core.IppTag.*
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URI
import java.time.Duration
import java.time.ZonedDateTime
import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.Logger.getLogger

@SuppressWarnings("kotlin:S1192")
class IppJob(
    val printer: IppPrinter,
    val attributes: IppAttributesGroup,
) {

    private val logger = getLogger(javaClass.name)
    var subscription: IppSubscription? = null

    constructor(printer: IppPrinter, response: IppResponse) : this(printer, response.jobGroup) {
        if (response.status != SuccessfulOk) logger.warning { "Job response status: ${response.status}" }
        if (response.containsGroup(Subscription)) subscription = IppSubscription(printer, response.subscriptionGroup)
    }

    companion object {
        var defaultDelay: Duration = Duration.ofSeconds(1)
    }

    var useJobOwnerAsUserName: Boolean = false
    var useJobUri: Boolean = false

    //--------------
    // IppAttributes
    //--------------

    val id: Int
        get() = attributes.getValue("job-id")

    val uri: URI
        get() = attributes.getValue("job-uri")

    val printerUri: URI
        get() = attributes.getValue("job-printer-uri")

    val state: JobState
        get() = JobState.fromAttributes(attributes)

    val stateReasons: List<String>
        get() = attributes.getValues("job-state-reasons")

    val name: IppString
        get() = attributes.getValue("job-name")

    val originatingUserName: IppString
        get() = attributes.getValue("job-originating-user-name")

    val originatingHostName: IppString
        get() = attributes.getValue("job-originating-host-name")

    val impressionsCompleted: Int
        get() = attributes.getValue("job-impressions-completed")

    val mediaSheetsCompleted: Int
        get() = attributes.getValue("job-media-sheets-completed")

    val kOctets: Int
        get() = attributes.getValue("job-k-octets")

    val pageRanges: Collection<IntRange>
        get() = attributes.getValues("page-ranges")

    val numberOfDocuments: Int
        get() = attributes.getValue("number-of-documents")

    val documentNameSupplied: IppString
        get() = attributes.getValue("document-name-supplied")

    val timeAtCreation: ZonedDateTime
        get() = attributes.getValueAsZonedDateTime("time-at-creation")

    val timeAtProcessing: ZonedDateTime
        get() = attributes.getValueAsZonedDateTime("time-at-processing")

    val timeAtCompleted: ZonedDateTime
        get() = attributes.getValueAsZonedDateTime("time-at-completed")

    val appleJobOwner: String // only supported by Apple CUPS
        get() = attributes.getValueAsString("com.apple.print.JobInfo.PMJobOwner")

    @JvmOverloads
    fun isPending(updateStateAttributes: Boolean = false) = stateIs(updateStateAttributes, Pending)

    @JvmOverloads
    fun isAborted(updateStateAttributes: Boolean = false) = stateIs(updateStateAttributes, Aborted)

    @JvmOverloads
    fun isCanceled(updateStateAttributes: Boolean = false) = stateIs(updateStateAttributes, Canceled)

    @JvmOverloads
    fun isCompleted(updateStateAttributes: Boolean = false) = stateIs(updateStateAttributes, Completed)

    @JvmOverloads
    fun isProcessing(updateStateAttributes: Boolean = false) = stateIs(updateStateAttributes, Processing)

    @JvmOverloads
    fun isProcessingStopped(updateStateAttributes: Boolean = false) = stateIs(updateStateAttributes, ProcessingStopped)

    @JvmOverloads
    fun isTerminated(updateStateAttributes: Boolean = false) = stateIs(updateStateAttributes, Canceled)
            || stateIs(false, Aborted) || stateIs(false, Completed)

    private fun stateIs(updateStateAttributes: Boolean, expectedState: JobState): Boolean {
        if (updateStateAttributes) updateAttributes("job-state", "job-state-reasons")
        return state == expectedState
    }

    // https://datatracker.ietf.org/doc/html/rfc8011#section-5.3.8
    fun stateReasonsContain(reason: String) = stateReasons.contains(reason)
    fun isProcessingToStopPoint() = stateReasonsContain("processing-to-stop-point")
    fun resourcesAreNotReady() = stateReasonsContain("resources-are-not-ready")
    fun isIncoming() = stateReasonsContain("job-incoming")

    fun getOriginatingUserNameOrAppleJobOwnerOrNull() = when {
        attributes.containsKey("job-originating-user-name") -> originatingUserName.text
        attributes.containsKey("com.apple.print.JobInfo.PMJobOwner") -> appleJobOwner
        else -> null
    }

    fun getJobNameOrDocumentNameSuppliedOrAppleJobNameOrNull() = when {
        attributes.containsKey("job-name") -> name.text
        attributes.containsKey("document-name-supplied") -> documentNameSupplied.text
        attributes.containsKey("com.apple.print.JobInfo.PMJobName") -> attributes.getValueAsString("com.apple.print.JobInfo.PMJobName")
        else -> null
    }

    fun getNumberOfDocumentsOrDocumentCount(): Int = when {
        attributes.containsKey("number-of-documents") -> numberOfDocuments
        attributes.containsKey("document-count") -> attributes.getValue("document-count")
        else -> throw IppException("number-of-documents or document-count not found, try calling ippJob.updateAttributes() first")
    }

    //-------------------
    // Get-Job-Attributes
    //-------------------

    //  RFC 8011 4.3.4 groups: 'all', 'job-template', 'job-description'
    @JvmOverloads
    fun getJobAttributes(requestedAttributes: List<String>? = null) =
        exchange(ippRequest(GetJobAttributes, requestedAttributes)).jobGroup

    fun getJobAttributes(vararg requestedAttribute: String) =
        getJobAttributes(requestedAttribute.toList())

    @JvmOverloads
    fun updateAttributes(requestedAttributes: List<String>? = null) =
        attributes.put(getJobAttributes(requestedAttributes))

    fun updateAttributes(vararg requestedAttributes: String) =
        updateAttributes(requestedAttributes.toList())

    //------------------------------------------
    // Wait for terminal state (RFC 8011 5.3.7.)
    //------------------------------------------

    @JvmOverloads
    fun waitForTermination(delay: Duration = defaultDelay) {
        logger.info { "Wait for termination of job #$id" }
        var lastPrinterString = ""
        var lastJobString = toString()
        logger.info { lastJobString }
        while (!isTerminated()) {
            Thread.sleep(delay.toMillis()) // no coroutines (http, ssl and stream parsing also require JVM)
            updateAttributes()
            if (toString() != lastJobString) {
                lastJobString = toString()
                logger.info { lastJobString }
            }
            if (!isProcessing() || lastPrinterString.isNotEmpty()) {
                printer.updateAttributes(
                    "printer-state", "printer-state-reasons", "printer-state-message", "printer-is-accepting-jobs"
                )
                if (printer.toString() != lastPrinterString) {
                    lastPrinterString = printer.toString()
                    logger.info { lastPrinterString }
                }
            }
            if (isProcessing() && lastPrinterString.isNotEmpty()) lastPrinterString = ""
        }
        if (isAborted()) log(logger)
    }

    //-------------------
    // Job administration
    //-------------------

    fun hold() = exchange(ippRequest(HoldJob))
    fun release() = exchange(ippRequest(ReleaseJob))
    fun restart() = exchange(ippRequest(RestartJob))

    @JvmOverloads
    fun cancel(messageForOperator: String? = null): IppResponse { // RFC 8011 4.3.3
        if (isCanceled()) logger.warning { "Job #$id is already 'canceled'" }
        if (isProcessingToStopPoint()) logger.warning { "Job #$id is already 'processing-to-stop-point'" }
        val request = ippRequest(CancelJob).apply {
            messageForOperator?.let { operationGroup.attribute("message", TextWithoutLanguage, it) }
        }
        logger.info { "Cancel $this" }
        return exchange(request)
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
        val request = documentRequest(SendDocument, lastDocument, documentName, documentNaturalLanguage)
            .apply { documentInputStream = inputStream }
        attributes.put(exchange(request).jobGroup)
    }

    @JvmOverloads
    fun sendDocument(
        file: File,
        lastDocument: Boolean = true,
        documentName: String? = null,
        documentNaturalLanguage: String? = null
    ) = sendDocument(FileInputStream(file), lastDocument, documentName, documentNaturalLanguage)

    //----------------------
    // Send-URI (depreacted)
    //----------------------

    @JvmOverloads
    @SuppressWarnings("kotlin:S1133") // some old printers support this optional operation
    // Deprecated(message = "see https://ftp.pwg.org/pub/pwg/ipp/registrations/reg-ippdepuri10-20211215.pdf")
    fun sendUri(
        documentUri: URI,
        lastDocument: Boolean = true,
        documentName: String? = null,
        documentNaturalLanguage: String? = null
    ) {
        val request = documentRequest(SendURI, lastDocument, documentName, documentNaturalLanguage)
            .apply { operationGroup.attribute("document-uri", Uri, documentUri) }
        attributes.put(exchange(request).jobGroup)
    }

    private fun documentRequest(
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

    //--------------
    // CUPS-Move-Job
    //--------------

    fun cupsMoveJob(printerUri: URI) = exchange(
        ippRequest(CupsMoveJob).apply {
            require(uri.host == printerUri.host) { "Printer $printerUri must be managed by the same server as ${uri.host}" }
            createAttributesGroup(Job).attribute("job-printer-uri", Uri, printerUri)
        }
    )

    fun cupsMoveJob(ippPrinter: IppPrinter) =
        cupsMoveJob(ippPrinter.printerUri)

    //------------------------
    // Create-Job-Subscription
    //------------------------

    @JvmOverloads
    fun createJobSubscription(notifyEvents: List<String>? = null): IppSubscription {
        val request = ippRequest(CreateJobSubscriptions).apply {
            printer.checkNotifyEvents(notifyEvents)
            createSubscriptionAttributesGroup(notifyEvents, notifyJobId = id)
        }
        val subscriptionAttributes = exchange(request).subscriptionGroup
        return IppSubscription(printer, subscriptionAttributes).apply {
            subscription = this
            if (notifyEvents != null && !events.containsAll(notifyEvents)) {
                logger.warning { "Server ignored some notifyEvents $notifyEvents, subscribed events: $events" }
            }
        }
    }

    //-------------------------------------------------------------------------------------
    // Cups-Get-Document
    // Security aspects for this operation are configured in cupsd.conf!
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

    @JvmOverloads
    fun cupsGetDocument(documentNumber: Int = 1): IppDocument {
        logger.fine { "CupsGetDocument #$documentNumber for job #$id" }
        try {
            val response = exchange(ippRequest(CupsGetDocument)
                .apply { operationGroup.attribute("document-number", Integer, documentNumber) })
            return IppDocument(this, response.jobGroup, response.documentInputStream!!)
        } catch (httpPostException: HttpPostException) {
            throw if (httpPostException.httpStatus == 426)
                IppException("Server requires TLS encrypted connection", httpPostException)
            else
                httpPostException
        }
    }

    @JvmOverloads
    fun cupsGetDocuments(
        save: Boolean = false,
        directory: File = printer.printerDirectory,
        optionalCommandToHandleFile: String? = null
    ) =
        (1..getNumberOfDocumentsOrDocumentCount())
            .map { cupsGetDocument(it) }
            .onEach { document ->
                if (save) with(document) {
                    save(directory, overwrite = true)
                    optionalCommandToHandleFile?.let { runtimeExecCommand(it) }
                }
            }

    //-----------------------
    // Delegate to IppPrinter
    //-----------------------

    private fun ippRequest(operation: IppOperation, requestedAttributes: List<String>? = null) =
        printer.ippRequest(
            operation, requestedAttributes, printerUri = null,
            userName = when {
                useJobOwnerAsUserName && attributes.containsKey("job-originating-user-name") -> originatingUserName.text
                useJobOwnerAsUserName && attributes.containsKey("com.apple.print.JobInfo.PMJobOwner") -> appleJobOwner
                else -> printer.ippConfig.userName
            }
        ).apply {
            operationGroup.run {
                if (useJobUri) {
                    // depending on network and CUPS config job uris might not be reachable
                    attribute("job-uri", Uri, uri)
                } else {
                    // play save, this uri has worked before
                    attribute("printer-uri", Uri, printer.printerUri)
                    attribute("job-id", Integer, id)
                }
            }
        }

    private fun exchange(request: IppRequest) = printer.exchange(request)

    // -------
    // Logging
    // -------

    @SuppressWarnings("kotlin:S3776")
    override fun toString(): String = attributes.run {
        StringBuilder("Job #$id").run {
            if (containsKey("job-state")) append(", state=$state")
            if (containsKey("job-state-reasons")) append(" (reasons=${stateReasons.joinToString(",")})")
            if (containsKey("job-name")) append(", name=$name")
            if (containsKey("job-impressions-completed")) append(", impressions-completed=$impressionsCompleted")
            if (containsKey("job-originating-host-name")) append(", originating-host-name=$originatingHostName")
            if (containsKey("job-originating-user-name") && get("job-originating-user-name")!!.tag.isValueTagAndIsNotOutOfBandTag())
                append(", originating-user-name=$originatingUserName")
            if (containsKey("com.apple.print.JobInfo.PMJobOwner")) append(", appleJobOwner=$appleJobOwner")
            if (containsKey("number-of-documents") || containsKey("document-count"))
                append(", ${getNumberOfDocumentsOrDocumentCount()} documents")
            if (containsKey("job-printer-uri")) append(", printer-uri=$printerUri")
            if (containsKey("job-uri")) append(", uri=$uri")
            toString()
        }
    }

    @JvmOverloads
    fun log(logger: Logger, level: Level = Level.INFO) =
        attributes.log(logger, level, title = "JOB #$id")

}