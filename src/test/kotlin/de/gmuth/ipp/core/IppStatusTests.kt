package de.gmuth.ipp.core

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import kotlin.test.*

class IppStatusTests {

    @Test
    fun isSuccessful() {
        assertTrue(IppStatus.SuccessfulOk.isSuccessful())
        assertFalse(IppStatus.ClientErrorBadRequest.isSuccessful())
    }

    @Test
    fun isClientError() {
        assertTrue(IppStatus.ClientErrorBadRequest.isClientError())
        assertFalse(IppStatus.ServerErrorInternalError.isClientError())
    }

    @Test
    fun isServerError() {
        assertTrue(IppStatus.ServerErrorInternalError.isServerError())
        assertFalse(IppStatus.ClientErrorBadRequest.isServerError())
    }

    @Test
    fun fromShort() {
        assertEquals(IppStatus.ClientErrorDocumentFormatNotSupported, IppStatus.fromInt(0x40A))
    }

    @Test
    fun fromShortFails() {
        assertFailsWith<IppException> { IppStatus.fromInt(10) }
    }

}