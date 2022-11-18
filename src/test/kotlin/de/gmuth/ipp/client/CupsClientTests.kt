package de.gmuth.ipp.client

/**
 * Copyright (c) 2020-2021 Gerhard Muth
 */

import de.gmuth.http.HttpClientMock
import de.gmuth.ipp.core.IppException
import de.gmuth.ipp.core.IppResponse
import de.gmuth.ipp.core.IppStatus.ClientErrorNotFound
import de.gmuth.ipp.core.IppStatus.SuccessfulOk
import de.gmuth.log.Logging
import org.junit.Test
import java.net.URI
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CupsClientTests {

    companion object {
        val log = Logging.getLogger { }
    }

    val httpClient = HttpClientMock()
    val cupsClient = CupsClient(URI.create("ipps://cups"), httpClient = httpClient)

    init {
        httpClient.ippResponse = IppResponse(SuccessfulOk) // default mocked response
    }

    @Test
    fun constructors() {
        CupsClient()
        CupsClient("host")
        CupsClient(config = IppConfig())
    }

    @Test
    fun getPrinters() {
        httpClient.mockResponse("CUPS/Cups-Get-Printers.ipp")
        cupsClient.getPrinters().forEach { log.info { it } }
    }

    @Test
    fun getPrinter() {
        httpClient.mockResponse("CUPS/Cups-Get-Printers.ipp")
        cupsClient.getPrinter("ColorJet_HP")
    }

    @Test
    fun getPrinterFails() {
        assertFailsWith<IppException> { // no such cups printer
            cupsClient.getPrinter("invalid")
        }
    }

    @Test
    fun getDefault() {
        httpClient.mockResponse("CUPS/Cups-Get-Default.ipp")
        cupsClient.getDefault()
    }

    @Test
    fun getDefaultFails() {
        val exception = assertFailsWith<IppExchangeException> {
            httpClient.mockResponse("CUPS/Cups-Get-Default-Error.ipp")
            cupsClient.getDefault()
        }
        assertTrue(exception.statusIs(ClientErrorNotFound))
    }

    @Test
    fun setDefault() {
        cupsClient.setDefault("matrix")
    }

}