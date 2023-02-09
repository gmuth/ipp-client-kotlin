package de.gmuth.ipp.client

import de.gmuth.http.HttpClientMock
import de.gmuth.ipp.core.IppOperation.GetPrinterAttributes
import de.gmuth.ipp.core.IppResponse
import de.gmuth.ipp.core.IppStatus.SuccessfulOk
import de.gmuth.log.Logging
import org.junit.Test
import java.net.URI

class IppClientTests {
    companion object {
        val log = Logging.getLogger { }
    }

    val httpClient = HttpClientMock()
    val ippClient = IppClient(httpClient = httpClient)

    init {
        httpClient.ippResponse = IppResponse(SuccessfulOk)
    }

    @Test
    fun sendRequestToURIWithEncodedWhitespaces() {
        val request = ippClient.ippRequest(GetPrinterAttributes, URI.create("ipp://localhost/printers/PDF%20Printer"))
        ippClient.exchange(request)
    }
}
