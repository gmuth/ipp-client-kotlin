package de.gmuth.ipp.client

import de.gmuth.ipp.core.IppOperation.GetPrinterAttributes
import org.junit.Test
import java.net.URI
import kotlin.test.assertEquals

class IppClientTests {
    val ippClient = IppClientMock()

    @Test
    fun sendRequestToURIWithEncodedWhitespaces() {
        ippClient.ippRequest(GetPrinterAttributes, URI.create("ipp://0/PDF%20Printer")).run {
            assertEquals("/PDF%20Printer", printerUri.rawPath)
            assertEquals("/PDF Printer", printerUri.path)
        }
    }
}
