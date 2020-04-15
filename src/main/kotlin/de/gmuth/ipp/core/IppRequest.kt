package de.gmuth.ipp.core

import java.nio.charset.Charset

/**
 * Copyright (c) 2020 Gerhard Muth
 */

class IppRequest() : IppMessage() {

    override val codeDescription: String
        get() = "operation = $operation"

    val operation: IppOperation
        get() = IppOperation.fromCode(code ?: throw IppException("operation-code must not be null"))

    val operationGroup = attributesGroup(IppTag.Operation)

    constructor(
            version: IppVersion,
            operation: IppOperation,
            requestId: Int,
            charset: Charset = Charsets.UTF_8,
            naturalLanguage: String = "en"

    ) : this() {
        this.version = version
        this.code = operation.code
        this.requestId = requestId
        operationGroup.attribute("attributes-charset", IppTag.Charset, charset.name().toLowerCase())
        operationGroup.attribute("attributes-natural-language", IppTag.NaturalLanguage, naturalLanguage)
    }

}