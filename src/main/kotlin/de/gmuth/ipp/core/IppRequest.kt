package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.nio.charset.Charset

open class IppRequest() : IppMessage() {

    override val codeDescription: String
        get() = "operation = $operation"

    private val operation: IppOperation
        get() = IppOperation.fromShort(code ?: throw IppException("operation-code must not be null"))

    constructor(
            version: IppVersion,
            operationCode: Short,
            requestId: Int,
            charset: Charset = Charsets.UTF_8,
            naturalLanguage: String = "en"

    ) : this() {
        this.version = version
        this.code = operationCode
        this.requestId = requestId
        with(ippAttributesGroup(IppTag.Operation)) {
            attribute("attributes-charset", IppTag.Charset, charset.name().toLowerCase())
            attribute("attributes-natural-language", IppTag.NaturalLanguage, naturalLanguage)
        }
    }

}