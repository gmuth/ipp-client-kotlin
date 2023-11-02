package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppOperation
import de.gmuth.ipp.core.IppRequest
import java.util.logging.Logger.getLogger
import kotlin.test.Test
import kotlin.test.assertEquals

class IppExchangeExceptionTests {

    private val logger = getLogger(javaClass.name)

    @Test
    fun constructor() {
        with(
            IppExchangeException(
                IppRequest(IppOperation.GetPrinterAttributes).apply { encode() },
                null, 400
        )
        ) {
            log(logger)
            assertEquals(11, request.code)
            assertEquals(400, httpStatus)
            assertEquals(message, "Get-Printer-Attributes failed")
        }
    }

}