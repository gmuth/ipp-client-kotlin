package de.gmuth.ipp.core

import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicInteger

/**
 * Copyright (c) 2020 Gerhard Muth
 */

open class IppRequest(operation: IppOperation) : IppMessage() {

    private val requestCounter = AtomicInteger(1)

    init {
        version = IppVersion()
        code = operation.code
        requestId = requestCounter.getAndIncrement()
        attributesCharset = Charsets.UTF_8
        naturalLanguage = "en"

        addOperationAttribute("attributes-charset", IppTag.Charset, (attributesCharset as Charset).name().toLowerCase())
        addOperationAttribute("attributes-natural-language", IppTag.NaturalLanguage, naturalLanguage as String)
    }

    override val codeDescription: String
        get() = "operation = $operation"

    val operation: IppOperation
        get() = IppOperation.fromShort(code ?: throw IllegalArgumentException("operation-code must not be null"))

}