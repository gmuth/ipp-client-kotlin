package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2024 Gerhard Muth
 */

import de.gmuth.ipp.attributes.*
import de.gmuth.ipp.attributes.CommunicationChannel.Companion.getCommunicationChannelsSupported
import de.gmuth.ipp.attributes.Marker.Companion.getMarkers
import de.gmuth.ipp.attributes.PrinterState.*
import de.gmuth.ipp.client.IppOperationException.ClientErrorNotFoundException
import de.gmuth.ipp.core.*
import de.gmuth.ipp.core.IppOperation.*
import de.gmuth.ipp.core.IppStatus.ClientErrorNotFound
import de.gmuth.ipp.core.IppTag.*
import de.gmuth.ipp.iana.IppRegistrationsSection2
import java.io.*
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.time.Instant.now
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.ofPattern
import java.util.logging.Level
import java.util.logging.Level.*
import java.util.logging.Logger
import java.util.logging.Logger.getLogger
import kotlin.io.path.createTempDirectory

@SuppressWarnings("kotlin:S1192")
open class IppPrinter(
    val printerUri: URI,
    val attributes: IppAttributesGroup = IppAttributesGroup(Printer),
    ippConfig: IppConfig = IppConfig(),
    val ippClient: IppClient = IppClient(ippConfig),
    getPrinterAttributesOnInit: Boolean = true,
    requestedAttributesOnInit: List<String>? = null
) {
    private val logger = getLogger(javaClass.name)
    lateinit var printerDirectory: File
    var throwIfSupportedAttributeIsNotAvailable: Boolean = true

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
            "output-bin-supported",
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
        logger.fine { "Create IppPrinter for $printerUri" }
        with(printerUri.scheme) {
            require(startsWith("ipp") || startsWith("http")) { "URI scheme unsupported: $this" }
        }
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
            } catch (ippOperationException: IppOperationException) {
                if (ippOperationException.statusIs(ClientErrorNotFound))
                    logger.severe { ippOperationException.message }
                else {
                    logger.severe { "Failed to get printer attributes on init. Workaround: getPrinterAttributesOnInit=false" }
                    ippOperationException.response.apply {
                        logger.warning { toString() } // IppClient logs request and response
                        if (containsGroup(Printer)) logger.warning { "${printerGroup.size} attributes parsed" }
                    }
                }
                throw ippOperationException
            }
        }
        initPrinterDirectory()
    }

    private fun initPrinterDirectory() {
        printerDirectory =
            if (attributes.isEmpty()) createTempDirectory().toFile()
            else File((if (isCups()) "CUPS_" else "") + makeAndModel.text.replace("\\s+".toRegex(), "_"))
        ippClient.saveMessagesDirectory = File(printerDirectory, ofPattern("HHmmss").format(LocalDateTime.now()))
    }

    constructor(printerAttributes: IppAttributesGroup, ippClient: IppClient) : this(
        printerAttributes.getValues<List<URI>>("printer-uri-supported").first(),
        printerAttributes,
        ippClient = ippClient
    )

    constructor(printerUri: String, ippConfig: IppConfig) :
            this(URI.create(printerUri), ippConfig = ippConfig)

    @JvmOverloads
    constructor(printerUri: String, getPrinterAttributesOnInit: Boolean = true) :
            this(URI.create(printerUri), getPrinterAttributesOnInit = getPrinterAttributesOnInit)

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
        get() = PrinterState.fromInt(attributes.getValue("printer-state"))

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

    val notifyEventsSupported: List<String>
        get() = attributes.getValues("notify-events-supported")

    val mediaSupported: List<String>
        get() = attributes.getValues("media-supported")

    val mediaReady: List<String>
        get() = attributes.getValues("media-ready")

    val mediaDefault: String
        get() = attributes.getValue("media-default")

    val mediaSourceSupported: List<String>
        get() = attributes.getValues("media-source-supported")

    val mediaTypeSupported: List<String>
        get() = attributes.getKeywordsOrNames("media-type-supported")

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

    val outputBinSupported: List<String>
        get() = attributes.getValues("output-bin-supported")

    val mediaSizeDefault: MediaSize
        get() = MediaSize.fromIppCollection(attributes.getValue("media-size-default"))

    val mediaSizeSupported: MediaSizeSupported
        get() = MediaSizeSupported.fromAttributes(attributes)

    val mediaColDefault: MediaCollection
        get() = MediaCollection.fromIppCollection(attributes.getValue("media-col-default"))

    val mediaColReady: List<MediaCollection>
        get() = attributes
            .getValues<List<IppCollection>>("media-col-ready")
            .map { MediaCollection.fromIppCollection(it) }

    fun getMediaColDatabase() = MediaColDatabase.fromAttributes(
        getPrinterAttributesOrNull("media-col-database")
            ?: throw IppException("Printer does not support media-col-database")
    )

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
        get() = attributes.getValue<IppString>("cups-version").text

    val supportedAttributes: Collection<IppAttribute<*>> = attributes.values
        .filter { it.name.endsWith("-supported") }
        .sortedBy { it.name }

    //-------------------------------------------------------

    private fun stateIs(updateStateAttributes: Boolean, expectedState: PrinterState): Boolean {
        if (updateStateAttributes) updateStateAttributes()
        return state == expectedState
    }

    fun isIdle(updateStateAttributes: Boolean = false) = stateIs(updateStateAttributes, Idle)
    fun isStopped(updateStateAttributes: Boolean = false) = stateIs(updateStateAttributes, Stopped)
    fun isProcessing(updateStateAttributes: Boolean = false) = stateIs(updateStateAttributes, Processing)

    internal fun anyStateReasonContains(reason: String) =
        stateReasons.any { it.contains(reason) } // support "...-error" and "...-report" values

    fun isPaused() = anyStateReasonContains("paused")
    fun isOffline() = anyStateReasonContains("offline")
    fun isTonerLow() = anyStateReasonContains("toner-low")
    fun isTonerEmpty() = anyStateReasonContains("toner-empty")
    fun isMediaJam() = anyStateReasonContains("media-jam")
    fun isMediaLow() = anyStateReasonContains("media-low")
    fun isMediaEmpty() = anyStateReasonContains("media-empty")
    fun isMediaNeeded() = anyStateReasonContains("media-needed")

    fun supportsOperations(vararg operations: IppOperation) = operationsSupported.containsAll(operations.toList())
    fun isDuplexSupported() = sidesSupported.any { it.startsWith("two-sided") }
    fun supportsVersion(version: String) = versionsSupported.contains(version)
    fun isCups() = attributes.containsKey("cups-version")

    fun isMediaSizeSupported(size: MediaSize) = mediaSizeSupported.supports(size)

    fun isMediaSizeReady(size: MediaSize) = mediaColReady
        .any { it.size?.equalsByDimensions(size) ?: false }

    fun sourcesOfMediaSizeReady(size: MediaSize) = mediaColReady
        .filter { it.size?.equalsByDimensions(size) ?: false }
        .map { it.source }

    //-----------------
    // Identify-Printer
    //-----------------

    // https://ftp.pwg.org/pub/pwg/candidates/cs-ippjobprinterext3v10-20120727-5100.13.pdf
    // https://ftp.pwg.org/pub/pwg/candidates/cs-ippnodriver20-20230301-5100.13.pdf

    fun identify(vararg actions: String) = identify(actions.toList())

    fun identify(actions: List<String>, message: String? = null): IppResponse {
        val request = ippRequest(IdentifyPrinter).apply {
            checkIfValueIsSupported("identify-actions", actions, true)
            operationGroup.attribute("identify-actions", Keyword, actions)
            message?.let { operationGroup.attribute("message", TextWithoutLanguage, it) }
        }
        return exchange(request)
    }

    fun flash() = identify("flash")
    fun sound() = identify("sound")
    fun display(message: String) = identify(listOf("display"), message = message)

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
        directory: File = printerDirectory,
        filename: String = "$makeAndModel.ppd"
    ) = File(directory, filename).also {
        cupsGetPPD(it.outputStream())
        logger.info { "Saved $it (${it.length()} bytes)" }
    }

    //------------------------------------------
    // Get-Printer-Attributes
    // names of attribute groups: RFC 8011 4.2.5
    //------------------------------------------

    fun getPrinterAttributesOrNull(requestedAttributes: Collection<String>? = null) =
        exchange(ippRequest(GetPrinterAttributes, requestedAttributes))
            .attributesGroups.singleOrNull { it.tag == Printer }

    fun getPrinterAttributesOrNull(vararg requestedAttributes: String) =
        getPrinterAttributesOrNull(requestedAttributes.toList())

    fun updateAttributes(requestedAttributes: List<String>? = null) =
        getPrinterAttributesOrNull(requestedAttributes)?.let { attributes.put(it) }

    fun updateAttributes(vararg requestedAttributes: String) =
        updateAttributes(requestedAttributes.toList())

    private lateinit var stateAttributesLastUpdated: Instant

    fun updateStateAttributes() {
        updateAttributes(
            "printer-state", "printer-state-reasons", "printer-state-message",
            "printer-is-accepting-jobs", "media-ready"
        )
        stateAttributesLastUpdated = now()
    }

    fun getAgeOfStateAttributes() = Duration.between(stateAttributesLastUpdated, now())
    fun stateAttributesAreOlderThan(duration: Duration) = getAgeOfStateAttributes() > duration

    //-------------
    // Validate-Job
    //-------------

    @Throws(IppExchangeException::class)
    fun validateJob(attributeBuilders: Collection<IppAttributeBuilder>) =
        exchange(attributeBuildersRequest(ValidateJob, attributeBuilders))

    @Throws(IppExchangeException::class)
    fun validateJob(vararg attributeBuilders: IppAttributeBuilder) =
        validateJob(attributeBuilders.toList())

    //--------------------------------------
    // Print-Job (with subscription support)
    //--------------------------------------

    @JvmOverloads
    fun printJob(
        inputStream: InputStream,
        attributeBuilders: Collection<IppAttributeBuilder>,
        notifyEvents: List<String>? = null // https://www.rfc-editor.org/rfc/rfc3995.html#section-5.3.3.4.3
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

    @JvmOverloads
    fun printJob(
        byteArray: ByteArray,
        attributeBuilders: Collection<IppAttributeBuilder>,
        notifyEvents: List<String>? = null
    ) =
        printJob(ByteArrayInputStream(byteArray), attributeBuilders, notifyEvents)

    @JvmOverloads
    fun printJob(
        file: File,
        attributeBuilders: Collection<IppAttributeBuilder>,
        notifyEvents: List<String>? = null
    ) =
        printJob(FileInputStream(file), attributeBuilders, notifyEvents)

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
        printJob(byteArray, attributeBuilders.toList(), notifyEvents)

    @JvmOverloads
    fun printJob(
        file: File,
        vararg attributeBuilders: IppAttributeBuilder,
        notifyEvents: List<String>? = null
    ) =
        printJob(file, attributeBuilders.toList(), notifyEvents)

    //-----------------------
    // Print-URI (deprecated)
    //-----------------------

    @SuppressWarnings("kotlin:S1133") // some old printers support this optional operation
    // Deprecated(message = "see https://ftp.pwg.org/pub/pwg/ipp/registrations/reg-ippdepuri10-20211215.pdf")
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
        for (attributeBuilder in attributeBuilders) with(attributeBuilder.build()) {
            checkIfValueIsSupported(name, values, false)
            // put attribute in operation or job group?
            val groupTag = IppRegistrationsSection2.selectGroupForAttribute(name) ?: Job
            if (!containsGroup(groupTag)) createAttributesGroup(groupTag)
            logger.finer { "$groupTag put $this" }
            getSingleAttributesGroup(groupTag).put(this)
        }
    }

    private fun IppAttributeBuilder.build() =
        buildIppAttribute(attributes)

    fun buildIppAttribute(attributeBuilder: IppAttributeBuilder) =
        attributeBuilder.buildIppAttribute(attributes)

    //-------------------------------
    // Get-Job-Attributes (as IppJob)
    //-------------------------------

    fun getJob(jobId: Int) = exchangeForIppJob(
        ippRequest(GetJobAttributes).apply {
            operationGroup.attribute("job-id", Integer, jobId)
        }
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
                    checkIfValueIsSupported("which-jobs", it, true)
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
        return IppSubscription(this, exchange(request).subscriptionGroup)
            .apply { logger.info { "Created $this" } }
    }

    fun checkNotifyEvents(notifyEvents: Collection<String>?) = notifyEvents?.let {
        if (attributes.isNotEmpty() && !attributes.containsKey("notify-events-supported"))
            throw IppException("Printer does not support event notifications.")
        if (it.isNotEmpty() && it.first() != "all") {
            checkIfValueIsSupported("notify-events", it, true)
        }
    }

    //-------------------------------------------------
    // Get-Subscription-Attributes (as IppSubscription)
    //-------------------------------------------------

    fun getSubscription(id: Int) = IppSubscription(
        this,
        exchange(ippRequest(GetSubscriptionAttributes).apply {
            operationGroup.attribute("notify-subscription-id", Integer, id)
        }).subscriptionGroup,
        startLease = false
    )

    //---------------------------------------------
    // Get-Subscriptions (as List<IppSubscription>)
    //---------------------------------------------

    fun getSubscriptions(
        notifyJobId: Int? = null,
        mySubscriptions: Boolean? = null,
        limit: Int? = null,
        requestedAttributes: Collection<String>? = null
    ): List<IppSubscription> {
        val request = ippRequest(GetSubscriptions, requestedAttributes = requestedAttributes).apply {
            operationGroup.run {
                notifyJobId?.let { attribute("notify-job-id", Integer, it) }
                mySubscriptions?.let { attribute("my-subscriptions", IppTag.Boolean, it) }
                limit?.let { attribute("limit", Integer, it) }
            }
        }
        return try {
            exchange(request)
                .getAttributesGroups(Subscription)
                .map { IppSubscription(this, it, startLease = false) }
        } catch (notFoundException: ClientErrorNotFoundException) {
            emptyList()
        }
    }

    //----------------------
    // delegate to IppClient
    //----------------------

    fun ippRequest(
        operation: IppOperation,
        requestedAttributes: Collection<String>? = null,
        userName: String? = ippConfig.userName,
        printerUri: URI? = this.printerUri,
        naturalLanguage: String = ippConfig.naturalLanguage
    ) = ippClient
        .ippRequest(operation, printerUri, requestedAttributes, userName, naturalLanguage)

    fun exchange(request: IppRequest): IppResponse = request.run {
        checkIfValueIsSupported("ipp-versions", version!!, throwIfSupportedAttributeIsNotAvailable)
        checkIfValueIsSupported("operations", code!!.toInt(), throwIfSupportedAttributeIsNotAvailable)
        checkIfValueIsSupported("charset", attributesCharset, throwIfSupportedAttributeIsNotAvailable)
        ippClient.exchange(this)
    }

    private fun exchangeForIppJob(request: IppRequest) =
        IppJob(this, exchange(request)).apply {
            if (request.containsGroup(Subscription) && subscription == null) {
                request.log(logger, WARNING, prefix = "REQUEST: ")
                val events: List<String> = request.subscriptionGroup.getValues("notify-events")
                throw IppException("printer/server did not create subscription for events: ${events.joinToString(",")}")
            }
        }

    private fun checkIfValueIsSupported(
        attributeName: String,
        value: Any,
        throwIfSupportedAttributeIsNotAvailable: Boolean
    ) = IppValueSupport.checkIfValueIsSupported(
        attributes,
        attributeName,
        value,
        throwIfSupportedAttributeIsNotAvailable
    )

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

    // ----------------------------------------------------------
    // Save printer attributes, printer icons and printer strings
    // ----------------------------------------------------------

    fun savePrinterAttributes() =
        exchange(ippRequest(GetPrinterAttributes)).run {
            saveBytes(File(printerDirectory, "${makeAndModel.text}.bin"))
            printerGroup.saveText(File(printerDirectory, "${makeAndModel.text}.txt"))
        }

    fun savePrinterIcons(): Collection<File> = attributes
        .getValues<List<URI>>("printer-icons")
        .map { it.save() }

    fun getPrinterStringsUri(language: String): URI {
        checkIfValueIsSupported("printer-strings-languages", language, false)
        return exchange(ippRequest(GetPrinterAttributes, listOf("printer-strings-uri"), naturalLanguage = language))
            .printerGroup.getValue("printer-strings-uri")
    }

    fun savePrinterStrings(language: String = "en") = try {
        getPrinterStringsUri(language).save(extension = "plist") // Apple property list
    } catch (fileNotFoundException: FileNotFoundException) {
        logger.warning { "Printer strings file not found: ${fileNotFoundException.message}" }
        null
    }

    fun saveAllPrinterStrings(): Collection<File>? = attributes["printer-strings-languages-supported"]
        ?.values?.mapNotNull { savePrinterStrings(it as String) }

    // --------------------------------------------------
    // Internal utilities implemented as Kotlin extension
    // --------------------------------------------------

    fun File.createDirectoryIfNotExists(throwOnFailure: Boolean = true) = this.apply {
        if (!mkdirs() && !isDirectory) "Failed to create directory: $path".let {
            if (throwOnFailure) throw IOException(it) else logger.warning(it)
        }
    }

    internal fun URI.save(
        directory: File? = printerDirectory.createDirectoryIfNotExists(),
        extension: String? = null,
        filename: String = path.substringAfterLast("/") + if (extension == null) "" else ".$extension"
    ) = File(directory, filename).also {
        toURL().openConnection().inputStream.copyTo(it.outputStream())
        logger.info { "Saved ${it.path} (${it.length()} bytes from $this)" }
    }
}