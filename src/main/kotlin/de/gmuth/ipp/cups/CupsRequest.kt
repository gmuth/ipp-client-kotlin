package de.gmuth.ipp.cups

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppException
import de.gmuth.ipp.core.IppRequest
import java.nio.charset.Charset
import java.util.*

class CupsRequest : IppRequest {

    override val codeDescription: String
        get() = "cupsOperation = $cupsOperation"

    private val cupsOperation: CupsOperation?
        get() = CupsOperation.fromShort(code ?: throw IppException("operation-code must not be null"))

    constructor(
            version: String,
            operation: CupsOperation,
            requestId: Int,
            charset: Charset = Charsets.UTF_8,
            naturalLanguage: Locale = Locale.ENGLISH

    ) : super(version, operation.code, requestId, charset, naturalLanguage)

}