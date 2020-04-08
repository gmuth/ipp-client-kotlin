package de.gmuth.ipp.core

import java.net.URI
import java.util.concurrent.atomic.AtomicInteger

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class IppRequest() : IppMessage() {

    init {
        version = IppVersion()
        requestId = requestCounter.getAndIncrement()
    }

    override val codeDescription: String
        get() = "operation = $operation"

    val operation: IppOperation
        get() = IppOperation.fromCode(code ?: throw IppException("operation-code must not be null"))

    val operationGroup = newAttributesGroup(IppTag.Operation)

    constructor(
            operation: IppOperation,
            printerUri: URI? = null,
            naturalLanguage: String = "en",
            requestingUserName: String? = System.getenv("USER")

    ) : this() {
        code = operation.code
        operationGroup.attribute("attributes-charset", IppTag.Charset, Charsets.UTF_8.name().toLowerCase())
        operationGroup.attribute("attributes-natural-language", IppTag.NaturalLanguage, naturalLanguage)

        if (printerUri != null) {
            operationGroup.attribute("printer-uri", IppTag.Uri, printerUri)
        }
        if (requestingUserName != null) {
            operationGroup.attribute("requesting-user-name", IppTag.NameWithoutLanguage, requestingUserName)
        }
    }

    fun newJobGroup() = newAttributesGroup(IppTag.Job)

    companion object {
        private val requestCounter = AtomicInteger(1)
    }

}