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
        documentFormat: String? = null,
        jobName: String? = null,
        jobParameters: List<IppJobParameter>? = null

) : IppRequest(IppOperation.PrintJob, printerUri) {

    init {
        if (documentFormat != null) operationGroup.attribute("document-format", IppTag.MimeMediaType, documentFormat)
        if (jobName != null) jobGroup.attribute("job-name", IppTag.NameWithoutLanguage, jobName)
        jobParameters?.forEach { jobParameter -> jobGroup.put(jobParameter.toIppAttribute()) }
    }

    constructor(printerUri: URI, file: File, documentFormat: String? = null, jobParameters: List<IppJobParameter>? = null) :
            this(printerUri, FileInputStream(file), documentFormat, file.name, jobParameters)

}