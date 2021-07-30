package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2021 Gerhard Muth
 */

import de.gmuth.ipp.core.IppOperation
import de.gmuth.ipp.core.IppOperation.*
import de.gmuth.ipp.core.IppTag.Printer
import java.net.URI

// https://www.cups.org/doc/spec-ipp.html
open class CupsClient(val cupsUri: URI) : IppClient() {

    constructor(host: String = "localhost", port: Int = 631) :
            this(URI.create(String.format("ipp://%s:%d", host, port)))

    // for convenient java usage
    constructor(host: String) : this(host, 631)

    init {
        if (cupsUri.scheme == "ipps") trustAnyCertificate()
    }

    protected fun ippRequest(operation: IppOperation) =
            ippRequest(operation, cupsUri)

    protected fun exchangeSuccessfulIppRequest(operation: IppOperation) =
            exchangeSuccessful(ippRequest(operation))

    fun setDefault(defaultPrinterUri: URI) =
            exchangeSuccessful(ippRequest(CupsSetDefault, defaultPrinterUri))

    fun getDefault() =
            IppPrinter(exchangeSuccessfulIppRequest(CupsGetDefault).printerGroup, this)

    fun getPrinters() =
            exchangeSuccessfulIppRequest(CupsGetPrinters)
                    .getAttributesGroups(Printer)
                    .map { IppPrinter(it, this) }

    fun getPrinter(name: String) =
            getPrinters().find { it.name.text == name } ?: throw NoSuchElementException(name)

}