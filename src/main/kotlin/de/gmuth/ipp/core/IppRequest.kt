package de.gmuth.ipp.core

import java.util.concurrent.atomic.AtomicInteger

/**
 * Copyright (c) 2020 Gerhard Muth
 */

open class IppRequest() : IppMessage() {

    private val requestCounter = AtomicInteger(1)

    init {
        version = IppVersion()
        requestId = requestCounter.getAndIncrement()
    }

    constructor(
            operation: IppOperation,
            naturalLanguage: String = "en"

    ) : this() {
        code = operation.code
        attributesCharset = Charsets.UTF_8
        addOperationAttribute("attributes-charset", IppTag.Charset, attributesCharset?.name()?.toLowerCase())
        addOperationAttribute("attributes-natural-language", IppTag.NaturalLanguage, naturalLanguage)
    }

    override val codeDescription: String
        get() = "operation = $operation"

    var operation: IppOperation
        get() = IppOperation.fromShort(code ?: throw IllegalArgumentException("operation-code must not be null"))
        set(operation) {
            code = operation.code
        }

}