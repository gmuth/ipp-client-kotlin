package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.ipp.client.IppExchangeException.ClientErrorNotFoundException
import de.gmuth.ipp.client.WhichJobs.All
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
    val cupsUri: URI = URI.create("ipp://localhost"),
    val ippClient: IppClient = IppClient()
) {
    constructor(host: String = "localhost") : this(URI.create("ipp://$host"))

    private val log = getLogger(javaClass.name)
    val config: IppConfig by ippClient::config
    var userName: String? by config::userName
    var cupsClientWorkDirectory = File("CUPS/${cupsUri.host}")

    init {
        if (cupsUri.scheme == "ipps") config.trustAnyCertificateAndSSLHostname()
    }

    fun getPrinters() = try {
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
                .apply { workDirectory = cupsClientWorkDirectory }
        } catch (clientErrorNotFoundException: ClientErrorNotFoundException) {
            with(getPrinters()) {
                if (isNotEmpty()) log.warning { "Available CUPS printers: ${map { it.name }}" }
            }
            throw clientErrorNotFoundException
        }

    fun getDefault() = IppPrinter(
        exchange(ippRequest(CupsGetDefault)).printerGroup, ippClient
    )

    fun setDefault(printerName: String) = exchange(
        cupsPrinterRequest(CupsSetDefault, printerName)
    )

    val version: String by lazy {
        getPrinters().run {
            if (isNotEmpty()) last()
            else IppPrinter(getJobs(All).last().printerUri)
        }.cupsVersion
    }

    internal fun cupsPrinterUri(printerName: String) = with(cupsUri) {
        val optionalPort = if (port > 0) ":$port" else ""
        URI("$scheme://$host$optionalPort/printers/$printerName")
    }.apply {
        log.fine { "cupsPrinterUri($printerName) -> $this" }
    }

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

    fun deletePrinter(printerName: String) = exchange(
        cupsPrinterRequest(CupsDeletePrinter, printerName)
    ).apply {
        log.info { "Printer deleted: $printerName" }
    }

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
            log.info { "$statusMessage ${printerGroup.getValues<Any>("printer-uri-supported")}" }
            return IppPrinter(printerGroup, ippClient)
        }
    }

    // --------------------------------------
    // Build request for a named CUPS printer
    // --------------------------------------

    protected fun cupsPrinterRequest(
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

    internal fun ippRequest(operation: IppOperation, printerURI: URI = cupsUri) =
        ippClient.ippRequest(operation, printerURI)

    internal fun exchange(ippRequest: IppRequest) =
        ippClient.exchange(ippRequest)

    fun basicAuth(user: String, password: String) =
        ippClient.basicAuth(user, password)

    //-----------------------
    // Delegate to IppPrinter
    //-----------------------

    protected val ippPrinter: IppPrinter by lazy {
        IppPrinter(cupsUri, ippClient = ippClient, getPrinterAttributesOnInit = false)
            .apply { workDirectory = cupsClientWorkDirectory }
    }

    fun getJobs(
        whichJobs: WhichJobs? = null,
        limit: Int? = null,
        requestedAttributes: List<String>? = ippPrinter.getJobsRequestedAttributes
    ) =
        ippPrinter.getJobs(whichJobs = whichJobs, limit = limit, requestedAttributes = requestedAttributes)

    //----------------------------
    // Create printer subscription
    //----------------------------

    fun createPrinterSubscription(
        // https://datatracker.ietf.org/doc/html/rfc3995#section-5.3.3.4.2
        notifyEvents: List<String>? = listOf("all"),
        notifyLeaseDuration: Duration? = null,
        notifyTimeInterval: Duration? = null
    ) =
        ippPrinter.createPrinterSubscription(notifyEvents, notifyLeaseDuration, notifyTimeInterval)

    fun createPrinterSubscription(
        vararg notifyEvents: String = arrayOf("all"),
        notifyLeaseDuration: Duration? = null,
        notifyTimeInterval: Duration? = null
    ) =
        createPrinterSubscription(notifyEvents.toList(), notifyLeaseDuration, notifyTimeInterval)

    //-----------------------------
    // Setup IPP Everywhere Printer
    //-----------------------------

    fun setupIppEverywherePrinter(
        printerName: String,
        deviceUri: URI,
        printerInfo: String? = null,
        printerLocation: String? = IppPrinter(deviceUri).location.text
    ) = createLocalPrinter(
        printerName,
        deviceUri,
        printerInfo,
        printerLocation,
        ppdName = "everywhere"
    ).apply {
        updateAttributes("printer-name")
        log.info(toString())
        require(deviceUri.scheme.startsWith("ipp")) { "uri scheme unsupported: $deviceUri" }
        log.info { "CUPS now generates IPP Everywhere PPD." }
        do { // https://github.com/apple/cups/issues/5919
            updateAttributes("printer-make-and-model")
        } while (!makeAndModel.text.lowercase().contains("everywhere"))
        log.info { "Make printer permanent." }
        exchange(
            cupsPrinterRequest(CupsAddModifyPrinter, printerName).apply {
                getSingleAttributesGroup(Printer).run {
                    attribute("printer-is-temporary", IppTag.Boolean, false)
                }
            }
        )
        log.info { "Make printer operational." }
        enable()
        resume()
        updateAttributes()
    }

    // ---------------------------
    // Get jobs and save documents
    // ---------------------------

    private val jobOwners = mutableSetOf<String>()

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
            // weird: do not modify above set
            // job-originating-user-name is missing when document-count or job-originating-host-name ist requested
            // once hidden in response, wait for one minute and user-name should show up again
        )
            .onEach { job -> // update attributes and lookup job owners
                if (updateJobAttributes) job.updateAttributes()
                log.info { job.toString() }
                job.getOriginatingUserNameOrAppleJobOwnerOrNull()?.let { jobOwners.add(it) }
            }
            .onEach { job -> // keep stats and save documents
                if (job.numberOfDocuments == 0) numberOfJobsWithoutDocuments.incrementAndGet()
                else getAndSaveDocuments(job, optionalCommandToHandleFile = commandToHandleSavedFile)
                    .apply { numberOfSavedDocuments.addAndGet(size) }
            }
            .apply {
                log.info { "Found ${jobOwners.size} job ${if (jobOwners.size == 1) "owner" else "owners"}: $jobOwners" }
                log.info { "Found $size jobs (which=$whichJobs) where $numberOfJobsWithoutDocuments jobs have no documents" }
                log.info { "Saved $numberOfSavedDocuments documents of ${size.minus(numberOfJobsWithoutDocuments.toInt())} jobs with documents to directory: ${ippPrinter.workDirectory}" }
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
        commandToHandleFile: String? = null // e.g. "open" -> open <filename> with Preview on MacOS
    ) {
        createPrinterSubscription(whichJobEvents, notifyLeaseDuration = leaseDuration)
            .pollAndHandleNotifications(pollEvery, autoRenewSubscription = autoRenewLease) { event ->
                log.info { event.toString() }
                with(event.getJob()) {
                    while (isIncoming()) {
                        log.info { toString() }
                        Thread.sleep(1000)
                        updateAttributes()
                    }
                    getAndSaveDocuments(this, optionalCommandToHandleFile = commandToHandleFile)
                }
            }
    }

    // ------------------------------
    // Get and save documents for job
    // ------------------------------

    internal fun getAndSaveDocuments(
        job: IppJob,
        onSuccessUpdateJobAttributes: Boolean = false,
        optionalCommandToHandleFile: String? = null
    ): Collection<File> {
        var documents: Collection<IppDocument> = emptyList()
        fun getDocuments() = try {
            documents = job.cupsGetDocuments()
            if (documents.isNotEmpty() && onSuccessUpdateJobAttributes) job.updateAttributes()
            true
        } catch (ippExchangeException: IppExchangeException) {
            log.info { "Get documents for job #${job.id} failed: ${ippExchangeException.message}" }
            ippExchangeException.httpStatus!! != 401
        }

        if (!getDocuments()) {
            val configuredUserName = config.userName
            jobOwners.forEach {
                config.userName = it
                log.fine { "set userName '${config.userName}'" }
                if (getDocuments()) return@forEach
            }
            config.userName = configuredUserName
        }

        documents.onEach { document ->
            document.save(job.printerDirectory(), overwrite = true)
            optionalCommandToHandleFile?.let { document.runCommand(it) }
        }
        return documents.map { it.file!! }
    }

}