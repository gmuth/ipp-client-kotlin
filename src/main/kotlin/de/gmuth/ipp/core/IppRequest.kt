package de.gmuth.ipp.core

import java.nio.charset.Charset

/**
 * Copyright (c) 2020 Gerhard Muth
 */

open class IppRequest(operation: IppOperation) : IppMessage() {

    init {
        version = IppVersion()
        code = operation.code
        attributesCharset = Charsets.UTF_8
        naturalLanguage = "en"

        operationGroup = IppAttributesGroup(IppTag.Operation)
        addOperationAttribute("attributes-charset", IppTag.Charset, (attributesCharset as Charset).name().toLowerCase())
        addOperationAttribute("attributes-natural-language", IppTag.NaturalLanguage, naturalLanguage as String)
    }

    override val codeDescription: String
        get() = "operation = $operation"

    val operation: IppOperation
        get() = IppOperation.fromShort(code ?: throw IllegalArgumentException("operation-code must not be null"))

}