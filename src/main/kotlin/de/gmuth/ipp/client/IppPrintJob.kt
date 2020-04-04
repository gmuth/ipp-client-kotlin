package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppOperation
import de.gmuth.ipp.core.IppRequest
import de.gmuth.ipp.core.IppTag
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URI

class IppPrintJob(
        val printerUri: URI,
        val documentInputStream: InputStream,
        val documentFormat: String? = null

) : IppRequest(IppOperation.PrintJob, printerUri) {

    init {
        if (documentFormat != null)
            operationGroup.attribute("document-format", IppTag.MimeMediaType, documentFormat)
    }

    constructor(
            printerUri: URI,
            file: File,
            documentFormat: String = "application/octet-stream"

    ) : this(printerUri, FileInputStream(file), documentFormat) {

        jobGroup.attribute("job-name", IppTag.NameWithoutLanguage, file.name)
    }

}