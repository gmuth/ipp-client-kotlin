package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2021 Gerhard Muth
 */

import de.gmuth.http.Http
import de.gmuth.ipp.client.IppPrinterState.*
import de.gmuth.ipp.core.*
import de.gmuth.ipp.core.IppOperation.*
import de.gmuth.ipp.core.IppStatus.ClientErrorNotFound
import de.gmuth.ipp.core.IppTag.*
import de.gmuth.ipp.iana.IppRegistrationsSection2
import de.gmuth.log.Logging
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URI

open class IppPrinter(
        val printerUri: URI,
        var attributes: IppAttributesGroup = IppAttributesGroup(Printer),
        httpConfig: Http.Config = Http.Config(),
        httpClient: Http.Client = Http.implementation.createHttpClient(httpConfig),
        ippConfig: IppConfig = IppConfig(),
        val ippClient: IppClient = IppClient(ippConfig, httpClient)
) {

    init {
        if (!ippConfig.getPrinterAttributesOnInit) {
            log.warn { "getPrinterAttributesOnInit disabled => no printer attributes available" }
        } else if (attributes.size == 0) {
            updateAllAttributes()
        }
    }

    constructor(printerAttributes: IppAttributesGroup, ippClient: IppClient = IppClient()) : this(
            printerAttributes.getValues<List<URI>>("printer-uri-supported").first(),
            printerAttributes,
            ippClient = ippClient
    )

    // constructors for java usage
    constructor(printerUri: String) : this(URI.create(printerUri))
    constructor(printerUri: String, ippConfig: IppConfig) : this(URI.create(printerUri), ippConfig = ippConfig)

    companion object {
        val log = Logging.getLogger {}
    }

    val ippConfig: IppConfig
        get() = ippClient.ippConfig

    var getJobsRequestedAttributes = listOf(
            "job-id", "job-uri", "job-printer-uri", "job-state", "job-name",
            "job-state-message", "job-state-reasons", "job-originating-user-name"
    )

    //---------------
    // ipp attributes
    //---------------

    val name: IppString
        get() = attributes.getValue("printer-name")

    val makeAndModel: IppString
        get() = attributes.getValue("printer-make-and-model")

    val isAcceptingJobs: Boolean
        get() = attributes.getValue("printer-is-accepting-jobs")

    val state: IppPrinterState
        get() = IppPrinterState.fromInt(attributes.getValue("printer-state"))

    val stateReasons: List<String>
        get() = attributes.getValues("printer-state-reasons")

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
                    add(IppCommunicationChannel(
                            printerUriSupported,
                            uriSecuritySupportedList[index],
                            uriAuthenticationSupportedList[index]
                    ))
            }
        }

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

    val markers: List<CupsMarker>
        get() = mutableListOf<CupsMarker>().apply {
            with(attributes) {
                val levels = getValues<List<Int>>("marker-levels")
                val lowLevels = getValues<List<Int>>("marker-low-levels")
                val highLevels = getValues<List<Int>>("marker-high-levels")
                val types = getValues<List<String>>("marker-types")
                val names = getValues<List<IppString>>("marker-names")
                val colors = getValues<List<IppString>>("marker-colors")
                for ((index, type) in types.withIndex())
                    add(CupsMarker(
                            type,
                            names[index].text,
                            levels[index],
                            lowLevels[index],
                            highLevels[index],
                            colors[index].text
                    ))
            }
        }

    fun marker(color: CupsMarker.Color) = markers.single { it.color == color }

    //-----------------

    fun isIdle() = state == Idle
    fun isStopped() = state == Stopped
    fun isProcessing() = state == Processing
    fun isMediaNeeded() = stateReasons.contains("media-needed")
    fun isDuplexSupported() = sidesSupported.any { it.startsWith("two-sided") }
    fun supportsOperations(vararg operations: IppOperation) = operationsSupported.containsAll(operations.toList())
    fun supportsVersion(version: String) = versionsSupported.contains(version)
    fun isCups() = attributes.containsKey("cups-version")

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

    fun pause() = exchange(ippRequest(PausePrinter))
    fun resume() = exchange(ippRequest(ResumePrinter))
    fun purgeJobs() = exchange(ippRequest(PurgeJobs))

    //-----------------------
    // Get-Printer-Attributes
    //-----------------------

    @JvmOverloads
    fun getPrinterAttributes(requestedAttributes: List<String>? = null) =
            exchange(ippRequest(GetPrinterAttributes, requestedAttributes = requestedAttributes))

    fun updateAllAttributes() {
        attributes = getPrinterAttributes().printerGroup
    }

    //-------------
    // Validate-Job
    //-------------

    @Throws(IppExchangeException::class)
    fun validateJob(vararg attributeBuilders: IppAttributeBuilder): IppResponse {
        val request = attributeBuildersRequest(ValidateJob, attributeBuilders)
        return exchange(request)
    }

    //----------
    // Print-Job
    //----------

    fun printJob(inputStream: InputStream, vararg attributeBuilder: IppAttributeBuilder, notifyEvents: List<String>? = null) =
            printInputStream(inputStream, attributeBuilder, notifyEvents)

    fun printJob(byteArray: ByteArray, vararg attributeBuilder: IppAttributeBuilder, notifyEvents: List<String>? = null) =
            printInputStream(ByteArrayInputStream(byteArray), attributeBuilder, notifyEvents)

    fun printJob(file: File, vararg attributeBuilder: IppAttributeBuilder, notifyEvents: List<String>? = null) =
            printInputStream(FileInputStream(file), attributeBuilder, notifyEvents)

    protected fun printInputStream(
            inputStream: InputStream,
            attributeBuilders: Array<out IppAttributeBuilder>,
            notifyEvents: List<String>? = null
    ): IppJob {
        val request = attributeBuildersRequest(PrintJob, attributeBuilders).apply {
            notifyEvents?.let { createSubscriptionGroup(this, notifyEvents) }
            documentInputStream = inputStream
        }
        return exchangeForIppJob(request)
    }

    //----------
    // Print-URI
    //----------

    fun printUri(documentUri: URI, vararg attributeBuilders: IppAttributeBuilder): IppJob {
        val request = attributeBuildersRequest(PrintURI, attributeBuilders).apply {
            operationGroup.attribute("document-uri", Uri, documentUri)
        }
        return exchangeForIppJob(request)
    }

    //-----------
    // Create-Job
    //-----------

    fun createJob(vararg attributeBuilders: IppAttributeBuilder): IppJob {
        val request = attributeBuildersRequest(CreateJob, attributeBuilders)
        return exchangeForIppJob(request)
    }

    // ---- factory method for operations Validate-Job, Print-Job, Print-Uri, Create-Job

    protected fun attributeBuildersRequest(operation: IppOperation, attributeBuilders: Array<out IppAttributeBuilder>) =
            ippRequest(operation).apply {
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
            requestedAttributes: List<String> = getJobsRequestedAttributes
    ): List<IppJob> {
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

    //----------------------------
    // Create-Printer-Subscription
    //----------------------------

    fun createPrinterSubscription(
            notifyEvents: List<String>? = null,
            notifyLeaseDuration: Int? = null // seconds
    ): IppSubscription {
        val request = ippRequest(CreatePrinterSubscriptions).apply {
            createSubscriptionGroup(this, notifyEvents, notifyLeaseDuration)
        }
        val subscriptionAttributes = exchange(request).getSingleAttributesGroup(Subscription)
        return IppSubscription(this, subscriptionAttributes)
    }

    fun createSubscriptionGroup(
            request: IppRequest,
            notifyEvents: List<String>? = null,
            notifyLeaseDuration: Int? = null, // seconds
            notifyJobId: Int? = null
    ) =
            request.createAttributesGroup(Subscription).apply {
                attribute("notify-pull-method", Keyword, "ippget")
                notifyJobId?.let { attribute("notify-job-id", Integer, it) }
                notifyLeaseDuration?.let { attribute("notify-lease-duration", Integer, it) }
                notifyEvents?.let {
                    if (notifyEvents.isNotEmpty() && notifyEvents.first() != "all")
                        checkIfValueIsSupported("notify-events-supported", it)
                    attribute("notify-events", Keyword, it)
                }
            }

    //-------------------------------------------------
    // Get-Subscription-Attributes (as IppSubscription)
    //-------------------------------------------------

    fun getSubscription(id: Int) = IppSubscription(this,
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

    fun exchange(request: IppRequest): IppResponse {
        return ippClient.exchange(request.apply {
            checkIfValueIsSupported("ipp-versions-supported", version!!)
            checkIfValueIsSupported("operations-supported", code!!.toInt())
            checkIfValueIsSupported("charset-supported", attributesCharset)
        })
    }

    fun exchangeForIppJob(request: IppRequest): IppJob {
        if (request.containsGroup(Subscription) && !supportsOperations(CreateJobSubscriptions))
            log.warn { "printer does not support Create-Job-Subscription" }
        val response = exchange(request)
        if (request.containsGroup(Subscription) && !response.containsGroup(Subscription)) {
            request.logDetails("REQUEST: ")
            val events: List<String> = request.getSingleAttributesGroup(Subscription).getValues("notify-events")
            throw IppException("server did not create subscription for events: ${events.joinToString(",")}")
        }
        val subscriptionsAttributes = response.run {
            if (containsGroup(Subscription)) getSingleAttributesGroup(Subscription) else null
        }
        return IppJob(this, response.jobGroup, subscriptionsAttributes)
    }

    // -------
    // Logging
    // -------

    override fun toString() =
            "IppPrinter: name=$name, makeAndModel=$makeAndModel, state=$state, stateReasons=$stateReasons"

    fun logDetails() =
            attributes.logDetails(title = "PRINTER-$name ($makeAndModel), $state $stateReasons")

    // ------------------------------------------------------
    // attribute value checking based on printer capabilities
    // ------------------------------------------------------

    fun checkIfValueIsSupported(supportedAttributeName: String, value: Any) {
        if (attributes.size == 0) return

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

    fun savePrinterAttributes() {
        val printerModel: String = makeAndModel.text.replace("\\s+".toRegex(), "_")
        getPrinterAttributes().run {
            saveRawBytes(File("$printerModel.bin"))
            printerGroup.saveText(File("$printerModel.txt"))
        }
    }

    // -------------------------------------------------
    // Create Printer Subscription and log Notifications
    // -------------------------------------------------

    fun logNotifications(notifyLeaseDuration: Int = 60 * 10, notifyEvents: List<String> = listOf("all")) =
            createPrinterSubscription(notifyEvents, notifyLeaseDuration).run {
                log.info { this }
                try {
                    do {
                        Thread.sleep(1000)
                        getNotifications(onlyNewEvents = true).forEach { log.info { it } }
                    } while (true)
                } catch (exchangeException: IppExchangeException) {
                    if (exchangeException.statusIs(ClientErrorNotFound)) {
                        log.info { exchangeException.response!!.statusMessage }
                    } else {
                        throw exchangeException
                    }
                }
            }

}