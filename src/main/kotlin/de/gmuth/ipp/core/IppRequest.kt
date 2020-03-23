package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

open class IppRequest(operation: IppOperation? = null) : IppMessage() {

    init {
        code = operation?.code
    }

    val operation: IppOperation
        get() = IppOperation.fromShort(code ?: throw IllegalArgumentException("operation-code must not be null"))

    override fun getCodeDescription() = "operation = $operation"

}