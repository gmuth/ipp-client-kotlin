package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.*
import java.io.File
import java.io.FileInputStream
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

fun main(args: Array<String>) {
    if (args.size < 2) {
        println("usage: java -jar ippclient.jar <printer-uri> <file>")
        return
    }
    val printerUri = URI.create(args[0])
    val file = File(args[1])

    IppMessage.verbose = true
    IppClient(printerUri).printDocument(FileInputStream(file))
}