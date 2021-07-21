package de.gmuth.ipp.cups

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.client.IppClient
import de.gmuth.ipp.client.IppPrinter
import de.gmuth.ipp.core.IppOperation
import de.gmuth.ipp.core.IppOperation.*
import de.gmuth.ipp.core.IppTag.Printer
import java.net.URI

class CupsClient(val cupsUri: URI) : IppClient() {

    constructor(host: String = "localhost", port: Int = 631) :
            this(URI.create(String.format("ipp://%s:%d", host, port)))

    // for convenient java usage
    constructor(host: String) : this(host, 631)

    init {
        if (cupsUri.scheme == "ipps") trustAnyCertificate()
    }

    private fun ippRequest(operation: IppOperation) =
            ippRequest(operation, cupsUri)

    private fun exchangeSuccessfulIppRequest(operation: IppOperation) =
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