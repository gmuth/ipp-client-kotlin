package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppOperation
import de.gmuth.ipp.core.IppRequest
import java.net.URI

class IppGetJobAttributes() : IppRequest(IppOperation.GetJobAttributes) {

    constructor(printerUri: URI, id: Int) : this() {
        operationGroup.attribute("printer-uri", printerUri)
        operationGroup.attribute("job-id", id)
    }

    constructor(jobUri: URI) : this() {
        operationGroup.attribute("job-uri", jobUri)
    }

}