package de.gmuth.ipp

/**
 * Author: Gerhard Muth
 */

import de.gmuth.ipp.core.IppOperation
import de.gmuth.ipp.core.IppOutputStream
import de.gmuth.ipp.core.IppRequest
import de.gmuth.ipp.core.IppTag
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