package de.gmuth.ipp

/**
 * Author: Gerhard Muth
 */

import java.net.URI

class IppPrintJobOperation(
        private val printerUri: URI,
        private val documentFormat: String? = null

) : IppMessage() {

    init {
        operation = IppOperation.PrintJob
    }

    override fun writeOperationAttributes(ippOutputStream: IppOutputStream) {
        ippOutputStream.writeAttribute(IppTag.Uri, "printer-uri", "$printerUri")
        if (documentFormat != null) {
            ippOutputStream.writeAttribute(IppTag.MimeMediaType, "document-format", documentFormat)
        }
    }

}