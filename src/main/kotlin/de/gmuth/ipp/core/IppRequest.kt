package de.gmuth.ipp.core

import java.util.concurrent.atomic.AtomicInteger

/**
 * Copyright (c) 2020 Gerhard Muth
 */

open class IppRequest() : IppMessage() {

    private val requestCounter = AtomicInteger(1)
    val operationGroup = newAttributesGroup(IppTag.Operation)
    val jobGroup = newAttributesGroup(IppTag.Job)

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

        operationGroup.attribute("attributes-charset", IppTag.Charset, attributesCharset?.name()?.toLowerCase())
        operationGroup.attribute("attributes-natural-language", IppTag.NaturalLanguage, naturalLanguage)
    }

    override val codeDescription: String
        get() = "operation = $operation"

    var operation: IppOperation
        get() = IppOperation.fromCode(code ?: throw IppException("operation-code must not be null"))
        set(operation) {
            code = operation.code
        }

}