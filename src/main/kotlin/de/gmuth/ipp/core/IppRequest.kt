package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.SequenceInputStream
import java.nio.charset.Charset
import java.util.*

open class IppRequest() : IppMessage() {

    var documentInputStream: InputStream? = null

    override val codeDescription: String
        get() = "operation = $operation"

    val operation: IppOperation
        get() = IppOperation.fromShort(code ?: throw IppException("operation-code must not be null"))

    val inputStream: InputStream
        get() {
            val encodedInputStream = try {
                ByteArrayInputStream(encode())
            } catch (exception: Exception) {
                throw IppException("failed to encode ipp request", exception)
            }
            return if (documentInputStream == null) {
                if (operation.requiresDocument()) {
                    throw IppException("missing document for '$operation' operation")
                }
                encodedInputStream
            } else {
                if (!operation.requiresDocument()) {
                    throw IppException("found unexpected document for '$operation' operation")
                }
                SequenceInputStream(encodedInputStream, documentInputStream)
            }
        }

    constructor(
            version: String,
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