package de.gmuth.ipp.client

import de.gmuth.http.Http
import de.gmuth.log.Logging
import java.nio.charset.Charset
import javax.net.ssl.SSLSocketFactory

class IppConfig(
        timeout: Int = 30000, // milli seconds
        userAgent: String? = "ipp-client-kotlin/2.1",
        sslSocketFactory: SSLSocketFactory? = null,
        verifySSLHostname: Boolean = false,
        chunkedStreamingMode: Boolean = false,
        basicAuth: Http.BasicAuth? = null,
        var userName: String? = basicAuth?.user ?: System.getProperty("user.name"),
        var ippVersion: String = "1.1",
        var charset: Charset = Charsets.UTF_8,
        var naturalLanguage: String = "en",
        var getPrinterAttributesOnInit: Boolean = true

) : Http.Config(
        timeout,
        userAgent,
        basicAuth,
        sslSocketFactory,
        verifySSLHostname,
        chunkedStreamingMode
) {
    companion object {
        val log = Logging.getLogger {}
    }

    fun logDetails() {
        log.info { "timeout: $timeout" }
        log.info { "userAgent: $userAgent" }
        log.info { "verifySSLHostname: $verifySSLHostname" }
        log.info { "chunkedStreamingMode: $chunkedStreamingMode" }
        log.info { "userName: $userName" }
        log.info { "ippVersion: $ippVersion" }
        log.info { "charset: $charset" }
        log.info { "naturalLanguage: $naturalLanguage" }
        log.info { "getPrinterAttributesOnInit: $getPrinterAttributesOnInit" }
    }
}