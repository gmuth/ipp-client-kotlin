package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import de.gmuth.http.Http
import de.gmuth.ipp.core.IppOperation
import de.gmuth.ipp.core.IppOperation.*
import de.gmuth.ipp.core.IppRequest
import de.gmuth.ipp.core.IppTag
import de.gmuth.ipp.core.IppTag.*
import de.gmuth.ipp.core.toIppString
import de.gmuth.log.Logging
import java.io.File
import java.io.InputStream
import java.net.URI
import java.time.Duration

// https://www.cups.org/doc/spec-ipp.html
open class CupsClient(
    val cupsUri: URI = URI.create("ipp://localhost"),
    ippConfig: IppConfig = IppConfig(),
    httpClient: Http.Client = Http.defaultImplementation.createClient(Http.Config())
) {
    constructor(host: String = "localhost") : this(URI.create("ipp://$host"))

    companion object {
        val log = Logging.getLogger { }
    }

    init {
        if (cupsUri.scheme == "ipps") httpClient.config.apply {
            verifySSLHostname = false
            trustAnyCertificate()
        }
    }

    protected val ippClient = IppClient(ippConfig, httpClient)
    var userName: String? by ippClient.config::userName
    fun getIppServer() = ippClient.getHttpServer()

    fun getPrinters() = try {
        exchange(ippRequest(CupsGetPrinters))
            .getAttributesGroups(Printer)
            .map { IppPrinter(it, ippClient) }
    } catch (ippExchangeException: IppExchangeException) {
        if (ippExchangeException.isClientErrorNotFound()) emptyList()
        else throw ippExchangeException
    }

    fun getPrinter(printerName: String) =
        try {
            IppPrinter(printerUri = cupsPrinterUri(printerName), ippClient = ippClient).apply {
                workDirectory = File("cups-${cupsUri.host}")
            }
        } catch (exception: IppExchangeException) {
            if (exception.isClientErrorNotFound()) with(getPrinters()) {
                if (isNotEmpty()) log.warn { "Available CUPS printers: ${map { it.name }}" }
            }
            throw exception
        }

    fun getDefault() = IppPrinter(
        exchange(ippRequest(CupsGetDefault)).printerGroup, ippClient
    )

    fun setDefault(printerName: String) =
        exchangeCupsPrinterRequest(CupsSetDefault, printerName)

    fun cupsPrinterUri(printerName: String) = with(cupsUri) {
        val optionalPort = if (port > 0) ":$port" else ""
        URI("$scheme://$host$optionalPort/printers/$printerName")
    }.apply {
        log.debug { "cupsPrinterUri($printerName) -> $this" }
    }

    // https://www.cups.org/doc/spec-ipp.html#CUPS_ADD_MODIFY_PRINTER
    fun addModifyPrinter(
        printerName: String,
        deviceUri: URI? = null,
        printerInfo: String? = null,
        printerLocation: String? = null,
        ppdName: String? = null, // virtual PPD 'everywhere' is not supported by all CUPS versions
        ppdInputStream: InputStream? = null,
        printerIsTemporary: Boolean? = null
    ) = exchangeCupsPrinterRequest(
        CupsAddModifyPrinter,
        printerName,
        deviceUri,
        ppdName,
        printerInfo,
        printerLocation,
        ppdInputStream,
        printerIsTemporary,
    )

    fun deletePrinter(printerName: String) =
        exchangeCupsPrinterRequest(CupsDeletePrinter, printerName)

    // https://www.cups.org/doc/spec-ipp.html#CUPS_CREATE_LOCAL_PRINTER
    fun createLocalPrinter(
        printerName: String,
        deviceUri: URI,
        printerInfo: String?,
        printerLocation: String?,
        ppdName: String? // virtual PPD 'everywhere' is supported asynchronous
    ) =
        exchangeCupsPrinterRequest(
            CupsCreateLocalPrinter,
            printerName,
            deviceUri,
            ppdName,
            printerInfo,
            printerLocation
        )

    // ---------------------------------------------------
    // build and exchange requests for a named CupsPrinter
    // ---------------------------------------------------

    protected fun exchangeCupsPrinterRequest(
        operation: IppOperation,
        printerName: String,
        deviceUri: URI? = null,
        ppdName: String? = null,
        printerInfo: String? = null,
        printerLocation: String? = null,
        ppdInputStream: InputStream? = null,
        printerIsTemporary: Boolean? = null,
    ) = exchange(
        ippRequest(operation, cupsPrinterUri(printerName)).apply {
            with(createAttributesGroup(Printer)) {
                attribute("printer-name", NameWithoutLanguage, printerName.toIppString())
                deviceUri?.let { attribute("device-uri", Uri, it) }
                ppdName?.let { attribute("ppd-name", NameWithoutLanguage, it.toIppString()) }
                printerInfo?.let { attribute("printer-info", TextWithoutLanguage, it.toIppString()) }
                printerLocation?.let { attribute("printer-location", TextWithoutLanguage, it.toIppString()) }
                printerIsTemporary?.let { attribute("printer-is-temporary", IppTag.Boolean, printerIsTemporary) }
            }
            ppdInputStream?.let { documentInputStream = ppdInputStream }
        }
    )

    //----------------------
    // delegate to IppClient
    //----------------------

    fun ippRequest(operation: IppOperation, printerURI: URI = cupsUri) =
        ippClient.ippRequest(operation, printerURI)

    fun exchange(ippRequest: IppRequest) =
        ippClient.exchange(ippRequest)

    fun basicAuth(user: String, password: String) =
        ippClient.basicAuth(user, password)

    //-----------------------
    // delegate to IppPrinter
    //-----------------------

    val ippPrinter: IppPrinter by lazy {
        IppPrinter(cupsUri, ippClient = ippClient, getPrinterAttributesOnInit = false)
    }

    fun createPrinterSubscription(
        // https://datatracker.ietf.org/doc/html/rfc3995#section-5.3.3.4.2
        notifyEvents: List<String>? = listOf("all"),
        notifyLeaseDuration: Duration? = Duration.ofMinutes(10)
    ) =
        ippPrinter.createPrinterSubscription(notifyEvents, notifyLeaseDuration)

    fun createPrinterSubscription(
        vararg notifyEvents: String = arrayOf("all"),
        notifyLeaseDuration: Duration? = null
    ) =
        createPrinterSubscription(notifyEvents.toList(), notifyLeaseDuration)

    //-----------------------------
    // Setup IPP Everywhere Printer
    //-----------------------------

    fun setupIppEverywherePrinter(
        printerName: String,
        deviceUri: URI,
        printerInfo: String? = null,
        printerLocation: String? = null
    ): IppPrinter {

        // validate ipp scheme
        if (!deviceUri.scheme.startsWith("ipp")) throw IllegalArgumentException(deviceUri.toString())

        createLocalPrinter(printerName, deviceUri, printerInfo, printerLocation, ppdName = "everywhere").apply {
            log.info { "$statusMessage ${printerGroup.getValues<List<URI>>("printer-uri-supported")}" }
        }

        return getPrinter(printerName).apply {

            // https://github.com/apple/cups/issues/5919
            log.info { "Waiting for CUPS to generate IPP Everywhere PPD." }
            log.info { this }
            do {
                Thread.sleep(1000)
                updateAttributes("printer-make-and-model")
            } while (!makeAndModel.text.lowercase().contains("everywhere"))
            log.info { this }

            // make printer permanent
            addModifyPrinter(printerName, printerIsTemporary = false)

            // make printer operational
            enable()
            resume()
            updateAttributes()
            log.info { this }
        }
    }
}