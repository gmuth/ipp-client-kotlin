package de.gmuth.ipp.client

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.core.IppOperation
import de.gmuth.ipp.core.IppRequest
import de.gmuth.ipp.core.IppResponse
import de.gmuth.ipp.core.IppStatus
import de.gmuth.log.Logging
import java.net.URI
import java.util.logging.Logger.getLogger
import kotlin.test.Test
import kotlin.test.assertEquals

class IppExchangeExceptionTests {

    init {
        Logging.configure()
    }

    private val logger = getLogger(javaClass.name)

    @Test
    fun operationException() {
        with(
            IppOperationException(
                IppRequest(IppOperation.GetPrinterAttributes).apply { encode() },
                IppResponse(status = IppStatus.ClientErrorBadRequest)
            )
        ) {
            log(logger)
            assertEquals(11, request.code)
            assertEquals("Get-Printer-Attributes failed: 'client-error-bad-request'", message)
        }
    }

    @Test
    fun httpPostException() {
        with(
            HttpPostException(
                IppRequest(
                    IppOperation.GetPrinterAttributes,
                    printerUri = URI.create("ipp://foo")
                ).apply { encode() },
                400
            )
        ) {
            log(logger)
            assertEquals(11, request.code)
            assertEquals(400, httpStatus)
            assertEquals( "http post for request Get-Printer-Attributes to ipp://foo failed", message)
        }
    }

}