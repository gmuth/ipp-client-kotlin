package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import de.gmuth.ipp.client.IppExchangeException
import kotlin.test.Test
import kotlin.test.assertEquals

class IppExchangeExceptionTests {

    @Test
    fun constructor() {
        with(IppExchangeException(
                IppRequest(IppOperation.GetPrinterAttributes),
                IppResponse().apply { code = IppStatus.SuccessfulOk.code },
                400,
                "ipp-exchange-failed"
        )) {
            logDetails()
            assertEquals(11, request.code)
            assertEquals(0, response?.code)
            assertEquals(400, httpStatus)
            assertEquals(message, "ipp-exchange-failed")
        }
    }

}