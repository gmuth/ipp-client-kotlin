package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2021 Gerhard Muth
 */

import de.gmuth.ipp.core.IppException
import de.gmuth.ipp.core.IppOperation
import de.gmuth.ipp.core.IppOperation.*
import de.gmuth.ipp.core.IppTag.Printer
import de.gmuth.log.Logging
import java.net.URI

// https://www.cups.org/doc/spec-ipp.html
open class CupsClient(val cupsUri: URI) : IppClient() {

    constructor(host: String = "localhost", port: Int = 631) : this(URI.create("ipp://$host:$port"))

    // for java
    constructor(host: String) : this(host, 631)

    companion object {
        val log = Logging.getLogger { }
    }

    init {
        if (cupsUri.scheme == "ipps") config.trustAnyCertificate()
    }

    val printerMap: Map<String, IppPrinter> by lazy {
        exchangeSuccessfulIppRequest(CupsGetPrinters)
                .getAttributesGroups(Printer)
                .map { IppPrinter(it, this) }
                .associateBy { it.name.text }
    }

    fun getPrinters() = printerMap.values

    fun getPrinter(name: String) =
            printerMap[name] ?: throw IppException("no such cups printer: $name").also {
                log.warn { "available printers: ${printerMap.keys.joinToString(",")}" }
            }

    protected fun ippRequest(operation: IppOperation) =
            ippRequest(operation, cupsUri)

    protected fun exchangeSuccessfulIppRequest(operation: IppOperation) =
            exchangeSuccessful(ippRequest(operation))

    fun setDefault(defaultPrinterUri: URI) =
            exchangeSuccessful(ippRequest(CupsSetDefault, defaultPrinterUri))

    fun getDefault() =
            IppPrinter(exchangeSuccessfulIppRequest(CupsGetDefault).printerGroup, this)

}