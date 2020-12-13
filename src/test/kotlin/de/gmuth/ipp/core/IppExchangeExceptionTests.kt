package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import kotlin.test.Test
import kotlin.test.assertEquals

class IppExchangeExceptionTests {

    @Test
    fun constructor() {
        with(IppExchangeException(
                IppRequest(IppOperation.GetPrinterAttributes),
                IppResponse().apply { code = IppStatus.SuccessfulOk.code },
                "ipp-exchange-failed"
        )) {
            logDetails()
            assertEquals(11, ippRequest.code)
            assertEquals(0, ippResponse.code)
            assertEquals(message, "ipp-exchange-failed")
        }
    }

}