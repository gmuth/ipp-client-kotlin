package de.gmuth.ipp

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.*
import java.io.InputStream
import java.net.URI

class IppPrintJobOperation(
        private val printerURI: URI,
        private val documentFormat: String? = null

) : IppRequest(IppOperation.PrintJob) {

    override fun writeOperationAttributes(ippOutputStream: IppOutputStream) {
        ippOutputStream.writeAttribute(IppTag.Uri, "printer-uri", "$printerURI")
        if (documentFormat != null) {
            ippOutputStream.writeAttribute(IppTag.MimeMediaType, "document-format", documentFormat)
        }
    }

}

fun IppClient.printDocument(inputStream: InputStream, documentFormat: String? = null): IppResponse {
    val ippRequest = IppPrintJobOperation(printerUri, documentFormat)
    val ippResponse = exchangeIpp(ippRequest, inputStream)
    if (!ippResponse.status.successfulOk()) {
        println("printing to $printerUri failed")
    }
    return ippResponse
}