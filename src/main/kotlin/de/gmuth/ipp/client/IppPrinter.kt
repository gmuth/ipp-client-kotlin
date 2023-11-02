package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.attributes.*
import de.gmuth.ipp.attributes.CommunicationChannel.Companion.getCommunicationChannelsSupported
import de.gmuth.ipp.attributes.Marker.Companion.getMarkers
import de.gmuth.ipp.attributes.PrinterState.*
import de.gmuth.ipp.core.*
import de.gmuth.ipp.core.IppOperation.*
import de.gmuth.ipp.core.IppStatus.ClientErrorNotFound
import de.gmuth.ipp.core.IppTag.*
import de.gmuth.ipp.iana.IppRegistrationsSection2
import java.io.*
import java.net.URI
import java.time.Duration
import java.util.logging.Level
import java.util.logging.Level.*
import java.util.logging.Logger
import java.util.logging.Logger.getLogger
import kotlin.io.path.createTempDirectory

@SuppressWarnings("kotlin:S1192")
class IppPrinter(
    val printerUri: URI,
    val attributes: IppAttributesGroup = IppAttributesGroup(Printer),
    ippConfig: IppConfig = IppConfig(),
    internal val ippClient: IppClient = IppClient(ippConfig),
    getPrinterAttributesOnInit: Boolean = true,
    requestedAttributesOnInit: List<String>? = null
) : IppExchange {

    private val logger = getLogger(javaClass.name)
    var workDirectory: File = createTempDirectory().toFile()

    companion object {

        val printerClassAttributes = listOf(
            "printer-name",
            "printer-make-and-model",
            "printer-info",
            "printer-location",
            "printer-is-accepting-jobs",
            "printer-state",
            "printer-state-reasons",
            "printer-state-message",
            "document-format-supported",
            "operations-supported",
            "color-supported",
            "sides-supported",
            "media-supported",
            "media-ready",
            "media-default",
            "media-source-supported",
            "ipp-versions-supported"
        )
    }

    init {
        logger.fine { "create IppPrinter for $printerUri" }
        require(printerUri.scheme.startsWith("ipp")) { "uri scheme unsupported: ${printerUri.scheme}" }
        if (printerUri.scheme == "ipps") ippConfig.trustAnyCertificateAndSSLHostname()
        if (!getPrinterAttributesOnInit) {
            logger.fine { "getPrinterAttributesOnInit disabled => no printer attributes available" }
        } else if (attributes.isEmpty()) {
            try {
                updateAttributes(requestedAttributesOnInit)
                if (isStopped()) {
                    logger.fine { toString() }
                    alert?.let { logger.info { "alert: $it" } }
                    alertDescription?.let { logger.info { "alert-description: $it" } }
                }
            } catch (ippExchangeException: IppExchangeException) {
                if (ippExchangeException.statusIs(ClientErrorNotFound))
                    logger.severe { ippExchangeException.message }
                else {
                    logger.severe { "Failed to get printer attributes on init. Workaround: getPrinterAttributesOnInit=false" }
                    ippExchangeException.response?.let {
                        if (it.containsGroup(Printer)) logger.info { "${it.printerGroup.size} attributes parsed" }
                        else logger.warning { "RESPONSE: $it" }
                    }
                }
                throw ippExchangeException
            }
            if (isCups()) workDirectory = File("cups-${printerUri.host}")
        }
    }

    constructor(printerAttributes: IppAttributesGroup, ippClient: IppClient) : this(
        printerAttributes.getValues<List<URI>>("printer-uri-supported").first(),
        printerAttributes,
        ippClient = ippClient
    )

    // constructors for java usage
    constructor(printerUri: String) : this(URI.create(printerUri))
    constructor(printerUri: String, ippConfig: IppConfig) : this(URI.create(printerUri), ippConfig = ippConfig)

    val ippConfig: IppConfig
        get() = ippClient.config

    fun basicAuth(user: String, password: String) =
        ippClient.basicAuth(user, password)

    var getJobsRequestedAttributes = mutableListOf(
        "job-id", "job-uri", "job-printer-uri", "job-state", "job-name",
        "job-state-reasons", "job-originating-user-name"
    )

    //---------------
    // IPP attributes
    //---------------

    val name: IppString
        get() = attributes.getValue("printer-name")

    val makeAndModel: IppString
        get() = attributes.getValue("printer-make-and-model")

    val info: IppString
        get() = attributes.getValue("printer-info")

    val location: IppString
        get() = attributes.getValue("printer-location")

    val isAcceptingJobs: Boolean
        get() = attributes.getValue("printer-is-accepting-jobs")

    val state: PrinterState
        get() = PrinterState.fromAttributes(attributes)

    val stateReasons: List<String>
        get() = attributes.getValues("printer-state-reasons")

    val stateMessage: IppString?
        get() = attributes.getValueOrNull("printer-state-message")

    val documentFormatSupported: List<String>
        get() = attributes.getValues("document-format-supported")

    val operationsSupported: List<IppOperation>
        get() = attributes.getValues<List<Int>>("operations-supported").map { IppOperation.fromInt(it) }

    val colorSupported: Boolean
        get() = attributes.getValue("color-supported")

    val sidesSupported: List<String>
        get() = attributes.getValues("sides-supported")

    val mediaSupported: List<String>
        get() = attributes.getValues("media-supported")

    val mediaReady: List<String>
        get() = attributes.getValues("media-ready")

    val mediaDefault: String
        get() = attributes.getValue("media-default")

    val mediaSourceSupported: List<String>
        get() = attributes.getValues("media-source-supported")

    val versionsSupported: List<String>
        get() = attributes.getValues("ipp-versions-supported")

    val communicationChannelsSupported: List<CommunicationChannel>
        get() = getCommunicationChannelsSupported(attributes)

    val alert: List<String>? // PWG 5100.9
        get() = attributes.getValuesOrNull("printer-alert")

    val alertDescription: List<IppString>? // PWG 5100.9
        get() = attributes.getValuesOrNull("printer-alert-description")

    val identifyActionsSupported: List<String>
        get() = attributes.getValues("identify-actions-supported")

    // ----------------------------------------------
    // Extensions supported by cups and some printers
    // https://www.cups.org/doc/spec-ipp.html
    // ----------------------------------------------

    val markers: Collection<Marker>
        get() = getMarkers(attributes)

    fun marker(color: Marker.Color) =
        markers.single { it.color == color }

    val deviceUri: URI
        get() = attributes.getValue("device-uri")

    val printerType: PrinterType
        get() = PrinterType.fromAttributes(attributes)

    fun hasCapability(capability: PrinterType.Capability) =
        printerType.contains(capability)

    val cupsVersion: String
        get() = attributes.getTextValue("cups-version")

    val supportedAttributes: Collection<IppAttribute<*>> = attributes.values
        .filter { it.name.endsWith("-supported") }
        .sortedBy { it.name }

    //-------------------------------------------------------

    fun isIdle() = state == Idle
    fun isStopped() = state == Stopped
    fun isProcessing() = state == Processing
    fun isMediaJam() = stateReasons.contains("media-jam")
    fun isMediaLow() = stateReasons.contains("media-low")
    fun isMediaEmpty() = stateReasons.contains("media-empty")
    fun isMediaNeeded() = stateReasons.contains("media-needed")
    fun isDuplexSupported() = sidesSupported.any { it.startsWith("two-sided") }
    fun supportsOperations(vararg operations: IppOperation) = operationsSupported.containsAll(operations.toList())
    fun supportsVersion(version: String) = versionsSupported.contains(version)
    fun isCups() = attributes.contains("cups-version")

    //-----------------
    // Identify-Printer
    //-----------------

    // https://ftp.pwg.org/pub/pwg/candidates/cs-ippjobprinterext3v10-20120727-5100.13.pdf
    fun identify(vararg actions: String) = identify(actions.toList())

    fun identify(actions: List<String>): IppResponse {
        val request = ippRequest(IdentifyPrinter).apply {
            checkIfValueIsSupported("identify-actions-supported", actions)
            operationGroup.attribute("identify-actions", Keyword, actions)
        }
        return exchange(request)
    }

    fun flash() = identify("flash")
    fun sound() = identify("sound")

    //-----------------------
    // Printer administration
    //-----------------------

    fun pause() = exchange(ippRequest(PausePrinter))
    fun resume() = exchange(ippRequest(ResumePrinter))
    fun purgeJobs() = exchange(ippRequest(PurgeJobs))
    fun enable() = exchange(ippRequest(EnablePrinter))
    fun disable() = exchange(ippRequest(DisablePrinter))
    fun holdNewJobs() = exchange(ippRequest(HoldNewJobs))
    fun releaseHeldNewJobs() = exchange(ippRequest(ReleaseHeldNewJobs))
    fun cancelJobs() = exchange(ippRequest(CancelJobs))
    fun cancelMyJobs() = exchange(ippRequest(CancelMyJobs))

    fun cupsGetPPD(copyTo: OutputStream? = null) = exchange(ippRequest(CupsGetPPD))
        .apply { copyTo?.let { documentInputStream!!.copyTo(it) } }

    fun savePPD(
        directory: File = workDirectory,
        filename: String = "$name.ppd",
        file: File = File(directory, filename)
    ) =
        file.also {
            cupsGetPPD(it.outputStream())
            logger.info { "Saved PPD: $it" }
        }

    //------------------------------------------
    // Get-Printer-Attributes
    // names of attribute groups: RFC 8011 4.2.5
    //------------------------------------------

    fun getPrinterAttributes(requestedAttributes: Collection<String>? = null) =
        exchange(ippRequest(GetPrinterAttributes, requestedAttributes = requestedAttributes)).printerGroup

    fun getPrinterAttributes(vararg requestedAttributes: String) =
        getPrinterAttributes(requestedAttributes.toList())

    fun updateAttributes(requestedAttributes: List<String>? = null) =
        attributes.put(getPrinterAttributes(requestedAttributes))

    fun updateAttributes(vararg requestedAttributes: String) =
        updateAttributes(requestedAttributes.toList())

    //-------------
    // Validate-Job
    //-------------

    @Throws(IppExchangeException::class)
    fun validateJob(vararg attributeBuilders: IppAttributeBuilder): IppResponse {
        val request = attributeBuildersRequest(ValidateJob, attributeBuilders.toList())
        return exchange(request)
    }

    //--------------------------------------
    // Print-Job (with subscription support)
    //--------------------------------------

    @JvmOverloads
    fun printJob(
        inputStream: InputStream,
        attributeBuilders: Collection<IppAttributeBuilder>,
        notifyEvents: List<String>? = null
    ): IppJob {
        val request = attributeBuildersRequest(PrintJob, attributeBuilders).apply {
            notifyEvents?.let {
                checkNotifyEvents(notifyEvents)
                createSubscriptionAttributesGroup(notifyEvents)
            }
            documentInputStream = inputStream
        }
        return exchangeForIppJob(request)
    }

    // vararg signatures for convenience

    @JvmOverloads
    fun printJob(
        inputStream: InputStream,
        vararg attributeBuilders: IppAttributeBuilder,
        notifyEvents: List<String>? = null
    ) =
        printJob(inputStream, attributeBuilders.toList(), notifyEvents)

    @JvmOverloads
    fun printJob(
        byteArray: ByteArray,
        vararg attributeBuilders: IppAttributeBuilder,
        notifyEvents: List<String>? = null
    ) =
        printJob(ByteArrayInputStream(byteArray), attributeBuilders.toList(), notifyEvents)

    @JvmOverloads
    fun printJob(file: File, vararg attributeBuilders: IppAttributeBuilder, notifyEvents: List<String>? = null) =
        printJob(FileInputStream(file), attributeBuilders.toList(), notifyEvents)

    //----------
    // Print-URI
    //----------

    fun printUri(documentUri: URI, vararg attributeBuilders: IppAttributeBuilder): IppJob {
        val request = attributeBuildersRequest(PrintURI, attributeBuilders.toList()).apply {
            operationGroup.attribute("document-uri", Uri, documentUri)
        }
        return exchangeForIppJob(request)
    }

    //-----------
    // Create-Job
    //-----------

    fun createJob(vararg attributeBuilders: IppAttributeBuilder): IppJob {
        val request = attributeBuildersRequest(CreateJob, attributeBuilders.toList())
        return exchangeForIppJob(request)
    }

    // ---- factory method for operations Validate-Job, Print-Job, Print-Uri, Create-Job

    protected fun attributeBuildersRequest(
        operation: IppOperation,
        attributeBuilders: Collection<IppAttributeBuilder>
    ) = ippRequest(operation).apply {
        for (attributeBuilder in attributeBuilders) {
            val attribute = attributeBuilder.buildIppAttribute(attributes)
            checkIfValueIsSupported("${attribute.name}-supported", attribute.values)
            // put attribute in operation or job group?
            val groupTag = IppRegistrationsSection2.selectGroupForAttribute(attribute.name) ?: Job
            if (!containsGroup(groupTag)) createAttributesGroup(groupTag)
            logger.finer { "$groupTag put $attribute" }
            getSingleAttributesGroup(groupTag).put(attribute)
        }
    }

    //-------------------------------
    // Get-Job-Attributes (as IppJob)
    //-------------------------------

    fun getJob(jobId: Int) =
        exchangeForIppJob(
            ippRequest(GetJobAttributes).apply { operationGroup.attribute("job-id", Integer, jobId) }
        )

    //---------------------------------
    // Get-Jobs (as Collection<IppJob>)
    //---------------------------------

    @JvmOverloads
    fun getJobs(
        whichJobs: WhichJobs? = null,
        myJobs: Boolean? = null,
        limit: Int? = null,
        requestedAttributes: List<String>? = getJobsRequestedAttributes
    ): Collection<IppJob> {
        logger.fine { "getJobs(whichJobs=$whichJobs, requestedAttributes=$requestedAttributes)" }
        val request = ippRequest(GetJobs, requestedAttributes = requestedAttributes).apply {
            operationGroup.run {
                whichJobs?.keyword?.let {
                    checkIfValueIsSupported("which-jobs-supported", it)
                    attribute("which-jobs", Keyword, it)
                }
                myJobs?.let { attribute("my-jobs", IppTag.Boolean, it) }
                limit?.let { attribute("limit", Integer, it) }
            }
        }
        return exchange(request)
            .getAttributesGroups(Job)
            .map { IppJob(this, it) }
    }

    fun getJobs(whichJobs: WhichJobs? = null, vararg requestedAttributes: String) =
        getJobs(whichJobs, requestedAttributes = requestedAttributes.toList())

    //------------
    // Cancel jobs
    //------------

    fun cancelJobs(whichJobs: WhichJobs) =
        getJobs(whichJobs).forEach { it.cancel() }

    //----------------------------
    // Create-Printer-Subscription
    //----------------------------

    fun createPrinterSubscription(
        // https://datatracker.ietf.org/doc/html/rfc3995#section-5.3.3.4.2
        notifyEvents: Collection<String>? = listOf("all"),
        notifyLeaseDuration: Duration? = null,
        notifyTimeInterval: Duration? = null
    ): IppSubscription {
        val request = ippRequest(CreatePrinterSubscriptions).apply {
            checkNotifyEvents(notifyEvents)
            createSubscriptionAttributesGroup(notifyEvents, notifyLeaseDuration, notifyTimeInterval)
        }
        val subscriptionAttributes = exchange(request).subscriptionGroup
        return IppSubscription(this, subscriptionAttributes)
    }

    fun checkNotifyEvents(notifyEvents: Collection<String>?) = notifyEvents?.let {
        if (it.isNotEmpty() && it.first() != "all")
            checkIfValueIsSupported("notify-events-supported", it)
    }

    //-------------------------------------------------
    // Get-Subscription-Attributes (as IppSubscription)
    //-------------------------------------------------

    fun getSubscription(id: Int) = IppSubscription(
        this,
        exchange(
            ippRequest(GetSubscriptionAttributes)
                .apply { operationGroup.attribute("notify-subscription-id", Integer, id) }
        )
            .subscriptionGroup
    )

    //---------------------------------------------
    // Get-Subscriptions (as List<IppSubscription>)
    //---------------------------------------------

    fun getSubscriptions(
        notifyJobId: Int? = null,
        mySubscriptions: Boolean? = null,
        limit: Int? = null,
        requestedAttributes: List<String>? = null
    ): List<IppSubscription> {
        val request = ippRequest(GetSubscriptions, requestedAttributes = requestedAttributes).apply {
            operationGroup.run {
                notifyJobId?.let { attribute("notify-job-id", Integer, it) }
                mySubscriptions?.let { attribute("my-subscriptions", IppTag.Boolean, it) }
                limit?.let { attribute("limit", Integer, it) }
            }
        }
        return exchange(request)
            .getAttributesGroups(Subscription)
            .map { IppSubscription(this, it) }
    }

    //----------------------
    // delegate to IppClient
    //----------------------

    internal fun ippRequest(
        operation: IppOperation,
        requestedAttributes: Collection<String>? = null,
        userName: String? = ippConfig.userName,
        printerUri: URI? = this.printerUri
    ) = ippClient
        .ippRequest(operation, printerUri, requestedAttributes, userName)

    override fun exchange(request: IppRequest) = ippClient.exchange(request.apply {
        checkIfValueIsSupported("ipp-versions-supported", version!!)
        checkIfValueIsSupported("operations-supported", code!!.toInt())
        checkIfValueIsSupported("charset-supported", attributesCharset)
    })

    private fun exchangeForIppJob(request: IppRequest) =
        IppJob(this, exchange(request)).apply {
            if (request.containsGroup(Subscription) && subscription == null) {
                request.log(logger, WARNING, prefix = "REQUEST: ")
                val events: List<String> = request.subscriptionGroup.getValues("notify-events")
                throw IppException("printer/server did not create subscription for events: ${events.joinToString(",")}")
            }
        }

    private fun checkIfValueIsSupported(supportedAttributeName: String, value: Any) =
        IppValueSupport.checkIfValueIsSupported(attributes, supportedAttributeName, value)

    // -------
    // Logging
    // -------

    override fun toString() = StringBuilder("Printer").run {
        if (attributes.containsKey("printer-name")) append(" $name")
        if (attributes.containsKey("printer-make-and-model")) append(" ($makeAndModel)")
        append(", state=$state, stateReasons=$stateReasons")
        stateMessage?.let { if (it.text.isNotEmpty()) append(", stateMessage=$stateMessage") }
        if (attributes.containsKey("printer-is-accepting-jobs")) append(", isAcceptingJobs=$isAcceptingJobs")
        if (attributes.containsKey("printer-location")) append(", location=$location")
        if (attributes.containsKey("printer-info")) append(", info=$info")
        toString()
    }

    @JvmOverloads
    fun log(logger: Logger, level: Level = INFO) =
        attributes.log(logger, level, title = "PRINTER $name ($makeAndModel)")

    // -----------------------
    // Save printer attributes
    // -----------------------

    fun savePrinterAttributes(directory: String = ".") {
        val printerModel: String = makeAndModel.text.replace("\\s+".toRegex(), "_")
        exchange(ippRequest(GetPrinterAttributes)).run {
            saveBytes(File(directory, "$printerModel.bin"))
            printerGroup.saveText(File(directory, "$printerModel.txt"))
        }
    }

    fun printerDirectory(printerName: String = name.text.replace("\\s+".toRegex(), "_")): File =
        File(workDirectory, printerName).createDirectoryIfNotExists()

    internal fun File.createDirectoryIfNotExists() = this
        .apply { if (!mkdirs() && !isDirectory) throw IOException("Failed to create directory: $path") }
}