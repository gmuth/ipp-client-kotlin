package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppOperation
import de.gmuth.ipp.core.IppRequest
import de.gmuth.ipp.core.IppTag
import java.net.URI

class IppGetJobAttributes() : IppRequest(IppOperation.GetJobAttributes) {

    constructor(printerUri: URI, id: Int) : this() {
        operationGroup.attribute("printer-uri", IppTag.Uri, printerUri)
        operationGroup.attribute("job-id", IppTag.Uri, id)
    }

    constructor(jobUri: URI) : this() {
        operationGroup.attribute("job-uri", IppTag.Uri, jobUri)
    }

}