package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.nio.charset.Charset

class IppRequest() : IppMessage() {

    override val codeDescription: String
        get() = "operation = $operation"

    val operation: IppOperation
        get() = IppOperation.fromShort(code ?: throw IppException("operation-code must not be null"))

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

        with(ippAttributesGroup(IppTag.Operation)) {
            attribute("attributes-charset", IppTag.Charset, charset.name().toLowerCase())
            attribute("attributes-natural-language", IppTag.NaturalLanguage, naturalLanguage)
        }
    }

}