package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2021 Gerhard Muth
 */

import de.gmuth.http.Http
import de.gmuth.ipp.core.IppException
import de.gmuth.ipp.core.IppOperation
import de.gmuth.ipp.core.IppOperation.*
import de.gmuth.ipp.core.IppTag.Printer
import de.gmuth.log.Logging
import java.net.URI

// https://www.cups.org/doc/spec-ipp.html
open class CupsClient(
        val cupsUri: URI = URI.create("ipp://localhost"),
        config: IppConfig = IppConfig(),
        httpClient: Http.Client = Http.defaultImplementation.createClient(Http.Config())

) : IppClient(config, httpClient) {

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

    val printerMap: Map<String, IppPrinter> by lazy {
        exchange(ippRequest(CupsGetPrinters))
                .getAttributesGroups(Printer)
                .map { IppPrinter(it, this) }
                .associateBy { it.name.text }
    }

    fun getPrinters() = printerMap.values

    fun getPrinter(name: String) =
            printerMap[name] ?: throw IppException("no such cups printer: $name").also {
                log.warn { "available printers: ${printerMap.keys.joinToString(",")}" }
            }

    protected fun ippRequest(operation: IppOperation) = ippRequest(operation, cupsUri)

    fun getDefault() =
            IppPrinter(exchange(ippRequest(CupsGetDefault)).printerGroup, this)

    fun setDefault(defaultPrinterUri: URI) =
            exchange(ippRequest(CupsSetDefault, defaultPrinterUri))

}