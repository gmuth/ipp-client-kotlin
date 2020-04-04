package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppOperation
import de.gmuth.ipp.core.IppRequest
import de.gmuth.ipp.core.IppTag
import java.net.URI

class IppPrintUri(
        printerUri: URI,
        documentUri: URI,
        documentFormat: String = "application/octet-stream",
        jobName: String = documentUri.path

) : IppRequest(IppOperation.PrintUri, printerUri) {

    init {
        operationGroup.attribute("document-uri", IppTag.Uri, documentUri)
        operationGroup.attribute("document-format", IppTag.MimeMediaType, documentFormat)
        jobGroup.attribute("job-name", IppTag.NameWithoutLanguage, jobName)
    }

}