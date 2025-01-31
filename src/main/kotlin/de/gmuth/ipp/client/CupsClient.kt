package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2025 Gerhard Muth
 */

import de.gmuth.ipp.client.IppOperationException.ClientErrorNotFoundException
import de.gmuth.ipp.client.WhichJobs.All
import de.gmuth.ipp.core.IppException
import de.gmuth.ipp.core.IppOperation
import de.gmuth.ipp.core.IppOperation.*
import de.gmuth.ipp.core.IppRequest
import de.gmuth.ipp.core.IppTag
import de.gmuth.ipp.core.IppTag.*
import java.io.File
import java.io.InputStream
import java.net.URI
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Logger.getLogger

// https://www.cups.org/doc/spec-ipp.html
class CupsClient(
    val cupsUri: URI = URI.create("ipps://localhost"),
    val ippClient: IppClient = IppClient()
) {
    @JvmOverloads
    constructor(printerUri: String = "ipps://localhost") : this(URI.create(printerUri))

    private val logger = getLogger(javaClass.name)
    val config: IppConfig by ippClient::config
    var userName: String? by config::userName
    var cupsDirectory = with(cupsUri) {
        File("CUPS" + (if (host in listOf("localhost", "127.0.0.1")) "-" else File.separator) + host)
    }

    private val cupsServer =
        IppPrinter(cupsUri, ippClient = ippClient, getPrinterAttributesOnInit = false)
            .apply { printerDirectory = cupsDirectory }

    init {
        if (cupsUri.scheme == "ipps") config.trustAnyCertificateAndSSLHostname()
    }

    private fun cupsPrinterUri(printerName: String) =
        cupsUri.run { URI("$scheme://$host${if (port > 0) ":$port" else ""}/printers/$printerName") }
            .apply { logger.finer { "cupsPrinterUri($printerName) = $this" } }

    val version: String by lazy {
        try {
            getPrinters().run {
                if (isNotEmpty()) last()
                else IppPrinter(getJobs(All).last().printerUri)
            }.cupsVersion
        } catch (exception: NoSuchElementException) {
            "?"
        }
    }

    fun getPrinters() =
        try {
            exchange(ippRequest(CupsGetPrinters))
                .getAttributesGroups(Printer)
                .map { IppPrinter(it, ippClient) }
        } catch (clientErrorNotFoundException: ClientErrorNotFoundException) {
            emptyList()
        }

    fun getPrinterNames() =
        getPrinters().map { it.name.toString() }

    fun printerExists(printerName: String) =
        getPrinterNames().contains(printerName)

    fun getPrinter(printerName: String) =
        try {
            IppPrinter(printerUri = cupsPrinterUri(printerName), ippClient = ippClient)
                .apply { printerDirectory = File(cupsDirectory, printerName).createDirectoryIfNotExists() }
        } catch (clientErrorNotFoundException: ClientErrorNotFoundException) {
            with(getPrinters()) {
                if (isNotEmpty()) logger.warning { "Available CUPS printers: ${map { it.name }}" }
            }
            throw clientErrorNotFoundException
        }

    fun getDefault() = IppPrinter(
        exchange(ippRequest(CupsGetDefault)).printerGroup, ippClient
    )

    fun setDefault(printerName: String) = exchange(
        cupsPrinterRequest(CupsSetDefault, printerName)
    )

    // https://www.cups.org/doc/spec-ipp.html#CUPS_ADD_MODIFY_PRINTER
    fun addModifyPrinter(
        printerName: String,
        deviceUri: URI? = null,
        printerInfo: String? = null,
        printerLocation: String? = null,
        ppdName: String? = null, // virtual PPD 'everywhere' is not supported by all CUPS versions
        ppdInputStream: InputStream? = null
    ) = exchange(
        cupsPrinterRequest(
            CupsAddModifyPrinter,
            printerName,
            deviceUri,
            printerInfo,
            printerLocation,
            ppdName,
            ppdInputStream
        )
    )

    fun addPrinterWithPPD(
        printerName: String,
        deviceUri: URI,
        printerInfo: String? = null,
        printerLocation: String? = null,
        ppdFile: File
    ) = addModifyPrinter(
        printerName,
        deviceUri,
        printerInfo,
        printerLocation,
        ppdInputStream = ppdFile.inputStream()
    ).run {
        getPrinter(printerName).apply {
            enable()
            resume()
            updateAttributes()
        }
    }

    fun deletePrinter(printerName: String) =
        exchange(cupsPrinterRequest(CupsDeletePrinter, printerName))
            .apply { logger.info { "Printer deleted: $printerName" } }

    // https://www.cups.org/doc/spec-ipp.html#CUPS_CREATE_LOCAL_PRINTER
    fun createLocalPrinter(
        printerName: String,
        deviceUri: URI,
        printerInfo: String?,
        printerLocation: String?,
        ppdName: String? // virtual PPD 'everywhere' is supported asynchronous
    ): IppPrinter {
        require(!printerName.contains("-")) { "printerName must not contain '-'" }
        exchange(
            cupsPrinterRequest(
                CupsCreateLocalPrinter,
                printerName,
                deviceUri,
                printerInfo,
                printerLocation,
                ppdName
            )
        ).run {
            logger.info { "$statusMessage ${printerGroup.getValues<Any>("printer-uri-supported")}" }
            return IppPrinter(printerGroup, ippClient)
        }
    }

    // --------------------------------------
    // Build request for a named CUPS printer
    // --------------------------------------

    private fun cupsPrinterRequest(
        operation: IppOperation,
        printerName: String,
        deviceUri: URI? = null,
        printerInfo: String? = null,
        printerLocation: String? = null,
        ppdName: String? = null,
        ppdInputStream: InputStream? = null
    ) =
        ippRequest(operation, cupsPrinterUri(printerName)).apply {
            with(createAttributesGroup(Printer)) {
                attribute("printer-name", NameWithoutLanguage, printerName)
                deviceUri?.let { attribute("device-uri", Uri, it) }
                ppdName?.let { attribute("ppd-name", NameWithoutLanguage, it) }
                printerInfo?.let { attribute("printer-info", TextWithoutLanguage, it) }
                printerLocation?.let { attribute("printer-location", TextWithoutLanguage, it) }
            }
            ppdInputStream?.let { documentInputStream = ppdInputStream }
        }

    //----------------------
    // Delegate to IppClient
    //----------------------

    fun basicAuth(user: String, password: String) =
        ippClient.basicAuth(user, password)

    private fun ippRequest(operation: IppOperation, printerURI: URI = cupsUri) =
        ippClient.ippRequest(operation, printerURI)

    private fun exchange(ippRequest: IppRequest) =
        ippClient.exchange(ippRequest).also { this.httpServer = it.httpServer }

    var httpServer: String? = null // from response after message exchange

    //---------
    // Get Jobs
    //---------

    fun getJobs(
        whichJobs: WhichJobs? = null,
        limit: Int? = null,
        requestedAttributes: List<String>? = cupsServer.getJobsRequestedAttributes
    ) =
        cupsServer.getJobs(whichJobs = whichJobs, limit = limit, requestedAttributes = requestedAttributes)

    fun getJob(id: Int) =
        cupsServer.getJob(id)

    //------------------
    // Get Subscriptions
    //------------------

    fun getSubscriptions() =
        cupsServer.getSubscriptions()

    fun getSubscription(id: Int) =
        cupsServer.getSubscription(id)

    fun getOwnersOfAllSubscriptions() =
        getSubscriptions()
            .map { it.subscriberUserName }
            .toSet()

    //--------------------
    // Create Subscription
    //--------------------

    fun createSubscription(
        // https://datatracker.ietf.org/doc/html/rfc3995#section-5.3.3.4.2
        notifyEvents: List<String>? = listOf("all"),
        notifyLeaseDuration: Duration? = null,
        notifyTimeInterval: Duration? = null
    ) =
        cupsServer.createPrinterSubscription(notifyEvents, notifyLeaseDuration, notifyTimeInterval)

    fun createSubscription(
        vararg notifyEvents: String = arrayOf("all"),
        notifyLeaseDuration: Duration? = null,
        notifyTimeInterval: Duration? = null
    ) =
        createSubscription(notifyEvents.toList(), notifyLeaseDuration, notifyTimeInterval)

    //-----------------------------
    // Create IPP Everywhere Printer
    //-----------------------------

    fun createIppEverywherePrinter(
        printerName: String,
        deviceUri: URI,
        printerInfo: String? = null,
        printerLocation: String? = // get location from ipp device
            IppPrinter(deviceUri, ippConfig = IppConfig().apply { trustAnyCertificateAndSSLHostname() }).location.text,
        savePPD: Boolean = false
    ) = createLocalPrinter(
        printerName,
        deviceUri,
        printerInfo,
        printerLocation,
        ppdName = "everywhere"
    ).apply {
        throwIfSupportedAttributeIsNotAvailable = false
        updateAttributes("printer-name")
        logger.info(toString())
        // https://github.com/apple/cups/issues/5919
        logger.info { "CUPS now should generate an everywhere PPD. Waiting for 'everywhere' in printer-make-and-model." }
        try {
            do {
                updateAttributes("printer-make-and-model")
                Thread.sleep(500)
            } while (!makeAndModel.text.lowercase().contains("everywhere"))
        } catch (exception: ClientErrorNotFoundException) {
            logger.warning { "Check your CUPS log files - it looks like the everywhere PPD wasn't generated..." }
            throw IppException("Failed to createIppEverywherePrinter() as described here: https://github.com/apple/cups/issues/5919")
        }
        logger.info { "Make printer permanent." }
        exchange(
            cupsPrinterRequest(CupsAddModifyPrinter, printerName)
                .apply { printerGroup.attribute("printer-is-temporary", IppTag.Boolean, false) }
        )
        logger.info { "Make printer operational." }
        enable()
        resume()
        updateAttributes()
        if (savePPD) savePPD()
    }

    // ---------------------------
    // Get jobs and save documents
    // ---------------------------

    fun getOwnersOfAllJobs() = getJobs(All)
        .mapNotNull { it.getOriginatingUserNameOrAppleJobOwnerOrNull() }
        .toSortedSet()

    fun getJobsAndSaveDocuments(
        whichJobs: WhichJobs = All,
        updateJobAttributes: Boolean = false,
        commandToHandleSavedFile: String? = null
    ): Collection<IppJob> {
        val numberOfJobsWithoutDocuments = AtomicInteger(0)
        val numberOfSavedDocuments = AtomicInteger(0)
        return getJobs(
            whichJobs,
            requestedAttributes = listOf(
                "job-id", "job-uri", "job-printer-uri", "job-originating-user-name",
                "job-name", "job-state", "job-state-reasons",
                if (version < "1.6.0") "document-count" else "number-of-documents"
            )
        )
            .onEach {
                if (updateJobAttributes) it.updateAttributes()
                logger.info { "$it" }
            }
            .onEach {
                it.apply { // job
                    if (getNumberOfDocumentsOrDocumentCount() == 0) {
                        numberOfJobsWithoutDocuments.incrementAndGet()
                    } else {
                        try {
                            useJobOwnerAsUserName = true
                            cupsGetDocuments(
                                save = true,
                                directory = File(cupsDirectory, printerUri.path.substringAfterLast("/")),
                                optionalCommandToHandleFile = commandToHandleSavedFile
                            )
                                .apply { numberOfSavedDocuments.addAndGet(size) }
                        } catch (ippExchangeException: IppExchangeException) {
                            logger.info { "Get documents for job #$id failed: ${ippExchangeException.message}" }
                        }
                    }
                }
            }
            .apply {
                logger.info { "Found $size jobs (which=$whichJobs) where $numberOfJobsWithoutDocuments jobs have no documents" }
                logger.info { "Saved $numberOfSavedDocuments documents of ${size.minus(numberOfJobsWithoutDocuments.toInt())} jobs with documents to directory: $cupsDirectory" }
            }
    }

    // -------------------------------------------------------------
    // Subscribe to job events and then get documents
    // https://www.rfc-editor.org/rfc/rfc3995.html#section-5.3.3.4.3
    // -------------------------------------------------------------

    fun subscribeToJobEventsAndThenGetDocuments(
        whichJobEvents: String = "job-created",
        leaseDuration: Duration = Duration.ofMinutes(60),
        autoRenewLease: Boolean = true,
        pollEvery: Duration = Duration.ofSeconds(1),
        saveDocuments: Boolean = true,
        commandToHandleFile: String? = null // e.g. "open" -> open <filename> with Preview on MacOS
    ) {
        createSubscription(whichJobEvents, notifyLeaseDuration = leaseDuration)
            .pollAndHandleNotifications(pollEvery, autoRenewSubscription = autoRenewLease) { event ->
                logger.info { event.toString() }
                with(event.getJob()) {
                    while (isIncoming()) {
                        logger.info { toString() }
                        Thread.sleep(1969)
                        updateAttributes()
                    }
                    useJobOwnerAsUserName = true
                    cupsGetDocuments(save = saveDocuments, optionalCommandToHandleFile = commandToHandleFile)
                }
            }
    }
}