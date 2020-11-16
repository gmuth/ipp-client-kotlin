package de.gmuth.ipp.cups

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.client.IppClient
import de.gmuth.ipp.client.IppPrinter
import de.gmuth.ipp.core.IppOperation
import de.gmuth.ipp.core.IppTag
import java.net.URI

class CupsClient(val cupsUri: URI) : IppClient() {

    constructor(host: String = "localhost", port: Int = 631) :
            this(URI.create(String.format("ipp://%s:%d", host, port)))

    private fun ippRequest(operation: IppOperation) = ippRequest(operation, cupsUri)

    private fun exchangeSuccessfulIppRequest(operation: IppOperation) = exchangeSuccessful(ippRequest(operation))

    fun setDefault(defaultPrinterUri: URI) =
            exchangeSuccessful(ippRequest(IppOperation.CupsSetDefault, defaultPrinterUri))

    fun getDefault(): IppPrinter =
            IppPrinter(exchangeSuccessfulIppRequest(IppOperation.CupsGetDefault).printerGroup, this)

    fun getPrinters(): List<IppPrinter> =
            exchangeSuccessfulIppRequest(IppOperation.CupsGetPrinters)
                    .getAttributesGroups(IppTag.Printer)
                    .map { printerAttributes -> IppPrinter(printerAttributes, this) }

    fun getPrinter(name: String) = getPrinters().first { it.name.string == name }

}