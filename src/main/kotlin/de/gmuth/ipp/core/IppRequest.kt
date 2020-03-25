package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

open class IppRequest(operation: IppOperation) : IppMessage() {

    init {
        version = IppVersion()
        code = operation.code
        attributesCharset = Charsets.UTF_8
        naturalLanguage = "en"
    }

    override val codeDescription: String
        get() = "operation = $operation"

    val operation: IppOperation
        get() = IppOperation.fromShort(code ?: throw IllegalArgumentException("operation-code must not be null"))

}