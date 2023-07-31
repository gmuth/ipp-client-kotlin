package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.http.Http
import de.gmuth.ipp.client.IppPrinterState.*
import de.gmuth.ipp.core.*
import de.gmuth.ipp.core.IppOperation.*
import de.gmuth.ipp.core.IppStatus.ClientErrorNotFound
import de.gmuth.ipp.core.IppTag.*
import de.gmuth.ipp.iana.IppRegistrationsSection2
import de.gmuth.log.Logging
import de.gmuth.log.Logging.LogLevel.ERROR
import java.io.*
import java.net.URI
import java.time.Duration

@SuppressWarnings("kotlin:S1192")
open class IppPrinter(
    val printerUri: URI,
    var attributes: IppAttributesGroup = IppAttributesGroup(Printer),
    httpConfig: Http.Config = Http.Config(),
    ippConfig: IppConfig = IppConfig(),
    val ippClient: IppClient = IppClient(ippConfig, Http.defaultImplementation.createClient(httpConfig)),
    getPrinterAttributesOnInit: Boolean = true,
    requestedAttributesOnInit: List<String>? = null
) {
    var workDirectory: File = File("work")

    init {
        log.debug { "create IppPrinter for $printerUri" }
        if (printerUri.scheme == "ipps") httpConfig.trustAnyCertificateAndSSLHostname()
        if (!getPrinterAttributesOnInit) {
            log.info { "getPrinterAttributesOnInit disabled => no printer attributes available" }
        } else if (attributes.isEmpty()) {
            try {
                updateAttributes(requestedAttributesOnInit)
                if (isStopped()) {
                    log.info { toString() }
                    alert?.let { log.info { "alert: $it" } }
                    alertDescription?.let { log.info { "alert-description: $it" } }
                }
            } catch (ippExchangeException: IppExchangeException) {
                if (ippExchangeException.statusIs(ClientErrorNotFound))
                    log.error { ippExchangeException.message }
                else {
                    log.logWithCauseMessages(ippExchangeException, ERROR)
                    log.error { "failed to get printer attributes on init" }
                    ippExchangeException.response?.let {
                        if (it.containsGroup(Printer)) log.info { "${it.printerGroup.size} attributes parsed" }
                        else log.warn { it }
                    }
                    try {
                        fetchRawPrinterAttributes("getPrinterAttributesFailed.bin")
                    } catch (exception: Exception) {
                        log.error(exception) { "failed to fetch raw printer attributes" }
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

    companion object {
        val log = Logging.getLogger {}

        val printerStateAttributes = listOf(
            "printer-is-accepting-jobs", "printer-state", "printer-state-reasons"
        )

        val printerClassAttributes = listOf(
            "printer-name",
            "printer-make-and-model",
            "printer-is-accepting-jobs",
            "printer-state",
            "printer-state-reasons",
            "document-format-supported",
            "operations-supported",
            "color-supported",
            "sides-supported",
            "media-supported",
            "media-ready",
            "media-default",
            "ipp-versions-supported"
        )
    }

    val ippConfig: IppConfig
        get() = ippClient.config

    var getJobsRequestedAttributes = mutableListOf(
        "job-id", "job-uri", "job-printer-uri", "job-state", "job-name",
        "job-state-reasons", "job-originating-user-name"
    )

    //---------------
    // ipp attributes
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

    val state: IppPrinterState
        get() = IppPrinterState.fromInt(attributes.getValue("printer-state"))

    val stateReasons: List<String>
        get() = attributes.getValues("printer-state-reasons")

    val stateMessage: IppString?
        get() = attributes.getValueOrNull("printer-state-message")

    val documentFormatSupported: List<String>
        get() = attributes.getValues("document-format-supported")

    val operationsSupported: List<IppOperation>
        get() = attributes.getValues<List<Int>>("operations-supported").map {
            IppOperation.fromShort(it.toShort())
        }

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

    val versionsSupported: List<String>
        get() = attributes.getValues("ipp-versions-supported")

    val communicationChannelsSupported: List<IppCommunicationChannel>
        get() = mutableListOf<IppCommunicationChannel>().apply {
            with(attributes) {
                val printerUriSupportedList = getValues<List<URI>>("printer-uri-supported")
                val uriSecuritySupportedList = getValues<List<String>>("uri-security-supported")
                val uriAuthenticationSupportedList = getValues<List<String>>("uri-authentication-supported")
                for ((index, printerUriSupported) in printerUriSupportedList.withIndex())
                    add(
                        IppCommunicationChannel(
                            printerUriSupported,
                            uriSecuritySupportedList[index],
                            uriAuthenticationSupportedList[index]
                        )
                    )
            }
        }

    val alert: List<String>? // PWG 5100.9
        get() = attributes.getValuesOrNull("printer-alert")

    val alertDescription: List<IppString>? // PWG 5100.9
        get() = attributes.getValuesOrNull("printer-alert-description")

    // ----------------------------------------------
    // extensions supported by cups and some printers
    // https://www.cups.org/doc/spec-ipp.html
    // ----------------------------------------------

    val deviceUri: URI
        get() = attributes.getValue("device-uri")

    val printerType: CupsPrinterType
        get() = CupsPrinterType(attributes.getValue("printer-type"))

    fun hasCapability(capability: CupsPrinterType.Capability) =
        printerType.contains(capability)

    val markers: Collection<CupsMarker>
        get() = with(attributes) {
            val types = getValues<List<String>>("marker-types")
            val names = getValues<List<IppString>>("marker-names")
            val levels = getValues<List<Int>>("marker-levels")
            val lowLevels = getValues<List<Int>>("marker-low-levels")
            val highLevels = getValues<List<Int>>("marker-high-levels")
            val colors = getValues<List<IppString>>("marker-colors")
            (0..types.size - 1).map {
                CupsMarker(
                    types[it],
                    names[it].text,
                    levels[it],
                    lowLevels[it],
                    highLevels[it],
                    colors[it].text
                )
            }
        }

    fun marker(color: CupsMarker.Color) = markers.single { it.color == color }

    //-----------------

    fun isIdle() = state == Idle
    fun isStopped() = state == Stopped
    fun isProcessing() = state == Processing
    fun isMediaEmpty() = stateReasons.contains("media-empty")
    fun isMediaNeeded() = stateReasons.contains("media-needed")
    fun isDuplexSupported() = sidesSupported.any { it.startsWith("two-sided") }
    fun supportsOperations(vararg operations: IppOperation) = operationsSupported.containsAll(operations.toList())
    fun supportsVersion(version: String) = versionsSupported.contains(version)
    fun isCups() = attributes.contains("cups-version")

    //-----------------
    // Identify-Printer
    //-----------------

    fun identify(vararg actions: String) = identify(actions.toList())

    fun identify(actions: List<String>): IppResponse {
        checkIfValueIsSupported("identify-actions-supported", actions)
        val request = ippRequest(IdentifyPrinter).apply {
            operationGroup.attribute("identify-actions", Keyword, actions)
        }
        return exchange(request)
    }

    fun flash() = identify("flash")
    fun sound() = identify("sound")

    //-----------------------
    // Printer administration
    //-----------------------

    fun pause() = exchangeIppRequest(PausePrinter)
    fun resume() = exchangeIppRequest(ResumePrinter)
    fun purgeJobs() = exchangeIppRequest(PurgeJobs)
    fun enable() = exchangeIppRequest(EnablePrinter)
    fun disable() = exchangeIppRequest(DisablePrinter)
    fun holdNewJobs() = exchangeIppRequest(HoldNewJobs)
    fun releaseHeldNewJobs() = exchangeIppRequest(ReleaseHeldNewJobs)
    fun cancelJobs() = exchangeIppRequest(CancelJobs)
    fun cancelMyJobs() = exchangeIppRequest(CancelMyJobs)

    //------------------------------------------
    // Get-Printer-Attributes
    // names of attribute groups: RFC 8011 4.2.5
    //------------------------------------------

    fun getPrinterAttributes(requestedAttributes: List<String>? = null) =
        exchange(ippRequest(GetPrinterAttributes, requestedAttributes = requestedAttributes))

    fun getPrinterAttributes(vararg requestedAttributes: String) =
        getPrinterAttributes(requestedAttributes.toList())

    fun updateAttributes(requestedAttributes: List<String>? = null) {
        log.debug { "update attributes: $requestedAttributes" }
        getPrinterAttributes(requestedAttributes).run {
            if (containsGroup(Printer)) attributes.put(printerGroup)
            else log.warn { "no printerGroup in response for requested attributes: $requestedAttributes" }
        }
    }

    fun updateAttributes(vararg requestedAttributes: String) =
        updateAttributes(requestedAttributes.toList())

    fun updatePrinterStateAttributes() =
        updateAttributes(printerStateAttributes)

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
            log.trace { "$groupTag put $attribute" }
            getSingleAttributesGroup(groupTag).put(attribute)
        }
    }

    //-------------------------------
    // Get-Job-Attributes (as IppJob)
    //-------------------------------

    fun getJob(jobId: Int): IppJob {
        val request = ippRequest(GetJobAttributes, jobId)
        return exchangeForIppJob(request)
    }

    //---------------------------
    // Get-Jobs (as List<IppJob>)
    //---------------------------

    @JvmOverloads
    fun getJobs(
        whichJobs: IppWhichJobs? = null,
        myJobs: Boolean? = null,
        limit: Int? = null,
        requestedAttributes: List<String>? = getJobsRequestedAttributes
    ): Collection<IppJob> {
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

    fun getJobs(whichJobs: IppWhichJobs? = null, vararg requestedAttributes: String) =
        getJobs(whichJobs, requestedAttributes = requestedAttributes.toList())

    //----------------------------
    // Create-Printer-Subscription
    //----------------------------

    fun createPrinterSubscription(
        // https://datatracker.ietf.org/doc/html/rfc3995#section-5.3.3.4.2
        notifyEvents: List<String>? = listOf("all"),
        notifyLeaseDuration: Duration? = null,
        notifyTimeInterval: Duration? = null
    ): IppSubscription {
        val request = ippRequest(CreatePrinterSubscriptions).apply {
            checkNotifyEvents(notifyEvents)
            createSubscriptionAttributesGroup(notifyEvents, notifyLeaseDuration, notifyTimeInterval)
        }
        val subscriptionAttributes = exchange(request).getSingleAttributesGroup(Subscription)
        return IppSubscription(this, subscriptionAttributes)
    }

    fun checkNotifyEvents(notifyEvents: List<String>?) = notifyEvents?.let {
        if (it.isNotEmpty() && it.first() != "all")
            checkIfValueIsSupported("notify-events-supported", it)
    }

    //-------------------------------------------------
    // Get-Subscription-Attributes (as IppSubscription)
    //-------------------------------------------------

    fun getSubscription(id: Int) = IppSubscription(
        this,
        exchange(ippRequest(GetSubscriptionAttributes).apply {
            operationGroup.attribute("notify-subscription-id", Integer, id)
        }).getSingleAttributesGroup(Subscription)
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

    fun ippRequest(operation: IppOperation, jobId: Int? = null, requestedAttributes: List<String>? = null) =
        ippClient.ippRequest(operation, printerUri, jobId, requestedAttributes)

    fun exchange(request: IppRequest) = ippClient.exchange(request.apply {
        checkIfValueIsSupported("ipp-versions-supported", version!!)
        checkIfValueIsSupported("operations-supported", code!!.toInt())
        checkIfValueIsSupported("charset-supported", attributesCharset)
    })

    protected fun exchangeIppRequest(operation: IppOperation) = exchange(ippRequest(operation))

    protected fun exchangeForIppJob(request: IppRequest): IppJob {
        val response = exchange(request)
        if (request.containsGroup(Subscription) && !response.containsGroup(Subscription)) {
            request.logDetails("REQUEST: ")
            val events: List<String> = request.getSingleAttributesGroup(Subscription).getValues("notify-events")
            throw IppException("printer/server did not create subscription for events: ${events.joinToString(",")}")
        }
        val subscriptionsAttributes = response.run {
            if (containsGroup(Subscription)) getSingleAttributesGroup(Subscription) else null
        }
        return IppJob(this, response.jobGroup, subscriptionsAttributes)
    }

    // -------
    // Logging
    // -------

    override fun toString() = StringBuilder("IppPrinter:").run {
        if (attributes.containsKey("printer-name")) append(" name=$name")
        append(", makeAndModel=$makeAndModel")
        append(", state=$state, stateReasons=$stateReasons")
        stateMessage?.let { if (it.text.isNotEmpty()) append(", stateMessage=$stateMessage") }
        if (attributes.containsKey("printer-is-accepting-jobs")) append(", isAcceptingJobs=$isAcceptingJobs")
        toString()
    }

    fun logDetails() =
        attributes.logDetails(title = "PRINTER-$name ($makeAndModel), $state $stateReasons")

    // ------------------------------------------------------
    // attribute value checking based on printer capabilities
    // ------------------------------------------------------

    fun checkIfValueIsSupported(supportedAttributeName: String, value: Any) {
        if (attributes.isEmpty()) return

        if (!supportedAttributeName.endsWith("-supported"))
            throw IppException("attribute name not ending with '-supported'")

        if (value is Collection<*>) { // instead of providing another signature just check collections iteratively
            for (collectionValue in value) {
                checkIfValueIsSupported(supportedAttributeName, collectionValue!!)
            }
        } else {
            isAttributeValueSupported(supportedAttributeName, value)
        }
    }

    fun isAttributeValueSupported(supportedAttributeName: String, value: Any): Boolean? {
        val supportedAttribute = attributes[supportedAttributeName] ?: return null
        val attributeValueIsSupported = when (supportedAttribute.tag) {
            IppTag.Boolean -> { // e.g. 'page-ranges-supported'
                supportedAttribute.value as Boolean
            }

            IppTag.Enum, Charset, NaturalLanguage, MimeMediaType, Keyword, Resolution -> when (supportedAttributeName) {
                "media-col-supported" -> with(value as IppCollection) {
                    members.filter { !supportedAttribute.values.contains(it.name) }
                        .forEach { log.warn { "member unsupported: $it" } }
                    // all member names must be supported
                    supportedAttribute.values.containsAll(members.map { it.name })
                }

                else -> supportedAttribute.values.contains(value)
            }

            Integer -> {
                if (supportedAttribute.is1setOf()) supportedAttribute.values.contains(value)
                else value is Int && value <= supportedAttribute.value as Int // e.g. 'job-priority-supported'
            }

            RangeOfInteger -> {
                value is Int && value in supportedAttribute.value as IntRange
            }

            else -> null
        }
        when (attributeValueIsSupported) {
            null -> log.warn { "unable to check if value '$value' is supported by $supportedAttribute" }
            true -> log.debug { "$supportedAttributeName: $value" }
            false -> {
                log.warn { "according to printer attributes value '${supportedAttribute.enumNameOrValue(value)}' is not supported." }
                log.warn { "$supportedAttribute" }
            }
        }
        return attributeValueIsSupported
    }

    // -----------------------
    // Save printer attributes
    // -----------------------

    fun savePrinterAttributes(directory: String = ".") {
        val printerModel: String = makeAndModel.text.replace("\\s+".toRegex(), "_")
        getPrinterAttributes().run {
            saveRawBytes(File(directory, "$printerModel.bin"))
            printerGroup.saveText(File(directory, "$printerModel.txt"))
        }
    }

    fun fetchRawPrinterAttributes(filename: String = "printer-attributes.bin") {
        ippClient.run {
            val httpResponse = httpPostRequest(toHttpUri(printerUri), ippRequest(GetPrinterAttributes))
            log.info { "http status: ${httpResponse.status}, content-type: ${httpResponse.contentType}" }
            File(filename).apply {
                httpResponse.contentStream!!.copyTo(outputStream())
                log.info { "saved ${length()} bytes: $path" }
            }
        }
    }

    fun printerDirectory(printerName: String = name.text.replace("\\s+".toRegex(), "_")) =
        File(workDirectory, printerName).apply {
            if (!mkdirs() && !isDirectory) throw IOException("failed to create printer directory: $path")
        }

}