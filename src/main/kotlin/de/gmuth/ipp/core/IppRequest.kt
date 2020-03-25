package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

open class IppRequest(operation: IppOperation) : IppMessage() {

    init {
        version = IppVersion()
        code = operation.code
        naturalLanguage = "en"
    }

    val operation: IppOperation
        get() = IppOperation.fromShort(code ?: throw IllegalArgumentException("operation-code must not be null"))

    override val codeDescription: String
        get() = "operation = $operation"

}