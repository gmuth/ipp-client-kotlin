package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2022 Gerhard Muth
 */

import de.gmuth.http.Http
import de.gmuth.ipp.core.*
import de.gmuth.ipp.core.IppOperation.*
import de.gmuth.ipp.core.IppTag.*
import de.gmuth.log.Logging
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

    val ippClient = IppClient(ippConfig, httpClient)

    fun getPrinters() = exchange(ippRequest(CupsGetPrinters))
        .getAttributesGroups(Printer)
        .map { IppPrinter(it, ippClient) }

    fun getPrinter(printerName: String): IppPrinter {
        val printerMap = getPrinters().associateBy { it.name.text }
        return printerMap[printerName] ?: throw IppException("no such cups printer: $printerName").also {
            log.warn { "available printers: ${printerMap.keys.joinToString(", ")}" }
        }
    }

    fun getDefault() =
        IppPrinter(exchange(ippRequest(CupsGetDefault)).printerGroup, ippClient)

    fun setDefault(printerName: String) =
        exchange(ippRequest(CupsSetDefault, cupsPrinterUri(printerName)))

    protected fun cupsPrinterUri(printerName: String) =
        with(cupsUri) { URI("$scheme://$host/printers/$printerName") }

    // https://www.cups.org/doc/spec-ipp.html#CUPS_ADD_MODIFY_PRINTER
    fun addModifyPrinter(
        printerName: String,
        deviceUri: URI,
        printerInfo: String?,
        printerLocation: String?,
        ppdName: String? = null, // virtual PPDs like "everywhere" are not supported by older CUPS versions
        ppdInputStream: InputStream? = null
    ): IppResponse {
        val ippRequest = ippRequest(CupsAddModifyPrinter, cupsPrinterUri(printerName)).apply {
            with(operationGroup) {
                attribute("device-uri", Uri, deviceUri)
                ppdName?.let { attribute("ppd-name", NameWithoutLanguage, it.toIppString()) }
                printerInfo?.let { attribute("printer-info", TextWithoutLanguage, it.toIppString()) }
                printerLocation?.let { attribute("printer-location", TextWithoutLanguage, it.toIppString()) }
                documentInputStream = ppdInputStream
            }
        }
        return exchange(ippRequest)
    }

    fun deletePrinter(printerName: String) =
        exchange(ippRequest(CupsDeletePrinter, cupsPrinterUri(printerName)))

    //----------------------
    // delegate to IppClient
    //----------------------

    fun ippRequest(operation: IppOperation, printerURI: URI = cupsUri) =
        ippClient.ippRequest(operation, printerURI)

    fun exchange(ippRequest: IppRequest) =
        ippClient.exchange(ippRequest)

    fun basicAuth(user: String, password: String) {
        ippClient.basicAuth(user, password)
    }

    //-----------------------
    // delegate to IppPrinter
    //-----------------------

    val ippPrinter = IppPrinter(cupsUri, ippClient = ippClient, getPrinterAttributesOnInit = false)

    fun createPrinterSubscription(
        // https://datatracker.ietf.org/doc/html/rfc3995#section-5.3.3.4.2
        notifyEvents: List<String>? = listOf("all"),
        notifyLeaseDuration: Duration? = null
    ) =
        ippPrinter.createPrinterSubscription(notifyEvents, notifyLeaseDuration)

    fun createPrinterSubscription(
        vararg notifyEvents: String = arrayOf("all"),
        notifyLeaseDuration: Duration? = null
    ) =
        createPrinterSubscription(notifyEvents.toList(), notifyLeaseDuration)

}