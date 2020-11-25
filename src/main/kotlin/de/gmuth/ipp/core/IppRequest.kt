package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.nio.charset.Charset

open class IppRequest : IppMessage {

    var documentInputStream: InputStream? = null

    override val codeDescription: String
        get() = "$operation"

    val operation: IppOperation
        get() = IppOperation.fromShort(code ?: throw IppException("operation-code must not be null"))

    val printerUri: URI
        get() = operationGroup.getValue("printer-uri") ?: throw IppException("missing printer-uri")

    override fun write(outputStream: OutputStream) {
        try {
            super.write(outputStream)
        } catch (exception: Exception) {
            throw IppException("failed to encode ipp request", exception)
        }
        when {
            operation.requiresDocument() && documentInputStream == null ->
                throw IppException("missing document for '$operation' operation")
            !operation.requiresDocument() && documentInputStream != null ->
                throw IppException("found unexpected document for '$operation' operation")
        }
        // append optional document
        documentInputStream?.copyTo(outputStream)
    }

    constructor(
            version: IppVersion = IppVersion(),
            requestId: Int = 1
    ) {
        this.version = version
        this.requestId = requestId
    }

    constructor(
            operation: IppOperation,
            printerUri: URI? = null,
            jobId: Int? = null,
            requestedAttributes: List<String>? = null,
            requestingUserName: String? = null,
            version: IppVersion = IppVersion(),
            requestId: Int = 1,
            charset: Charset = Charsets.UTF_8,
            naturalLanguage: String = "en"

    ) {
        this.version = version
        this.code = operation.code
        this.requestId = requestId

        with(ippAttributesGroup(IppTag.Operation)) {
            // required attributes
            attribute("attributes-charset", IppTag.Charset, charset)
            attribute("attributes-natural-language", IppTag.NaturalLanguage, naturalLanguage)
            // optional attributes
            printerUri?.let { attribute("printer-uri", IppTag.Uri, it) }
            jobId?.let { attribute("job-id", IppTag.Integer, it) }
            requestedAttributes?.let { attribute("requested-attributes", IppTag.Keyword, it) }
            requestingUserName?.let { attribute("requesting-user-name", IppTag.NameWithoutLanguage, it) }
        }
    }

}