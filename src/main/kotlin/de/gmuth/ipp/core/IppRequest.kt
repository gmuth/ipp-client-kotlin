package de.gmuth.ipp.core

/**
 * Copyright (c) 2020-2021 Gerhard Muth
 */

import de.gmuth.ipp.core.IppTag.*
import java.net.URI
import java.nio.charset.Charset

class IppRequest() : IppMessage() {

    val printerUri: URI
        get() = operationGroup.getValueOrNull("printer-uri") ?: throw IppException("missing 'printer-uri'")

    override val codeDescription: String
        get() = operation.toString()

    val operation: IppOperation
        get() = IppOperation.fromShort(code!!)

    constructor(
            operation: IppOperation,
            printerUri: URI? = null,
            jobId: Int? = null,
            requestedAttributes: List<String>? = null,
            requestingUserName: String? = null,
            version: String = "1.1",
            requestId: Int = 1,
            charset: Charset = Charsets.UTF_8,
            naturalLanguage: String = "en"
    ) : this() {

        this.version = version
        this.code = operation.code
        this.requestId = requestId

        with(createAttributesGroup(Operation)) {
            // required attributes
            attribute("attributes-charset", Charset, charset)
            attribute("attributes-natural-language", NaturalLanguage, naturalLanguage)
            // useful attributes
            jobId?.let { attribute("job-id", Integer, it) }
            printerUri?.let { attribute("printer-uri", Uri, it) }
            requestedAttributes?.let { attribute("requested-attributes", Keyword, it) }
            requestingUserName?.let { attribute("requesting-user-name", NameWithoutLanguage, it.toIppString()) }
        }
    }
}