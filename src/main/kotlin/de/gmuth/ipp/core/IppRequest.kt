package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.*

open class IppRequest() : IppMessage() {

    var documentInputStream: InputStream? = null

    override val codeDescription: String
        get() = "operation = $operation"

    val operation: IppOperation
        get() = IppOperation.fromShort(code ?: throw IppException("operation-code must not be null"))

    override fun write(outputStream: OutputStream) {
        try {
            super.write(outputStream)
        } catch (exception: Exception) {
            throw IppException("failed to encode ipp request", exception)
        }
        when {
            operation.requiresDocument() && documentInputStream == null -> throw IppException("missing document for '$operation' operation")
            !operation.requiresDocument() && documentInputStream != null -> throw IppException("found unexpected document for '$operation' operation")
        }
        // append document data
        if (documentInputStream != null) documentInputStream!!.copyTo(outputStream)
    }

    constructor(
            version: IppVersion,
            operationCode: Short,
            requestId: Int,
            charset: Charset = Charsets.UTF_8,
            naturalLanguage: Locale = Locale.ENGLISH
    ) : this() {
        this.version = version
        this.code = operationCode
        this.requestId = requestId
        with(ippAttributesGroup(IppTag.Operation)) {
            attribute("attributes-charset", IppTag.Charset, charset)
            attribute("attributes-natural-language", IppTag.NaturalLanguage, naturalLanguage)
        }
    }

}