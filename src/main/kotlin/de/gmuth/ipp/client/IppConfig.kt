package de.gmuth.ipp.client

/**
 * Copyright (c) 2021-2024 Gerhard Muth
 */

import java.nio.charset.Charset
import java.time.Duration
import java.util.Base64.getEncoder
import java.util.logging.Level
import java.util.logging.Level.INFO
import java.util.logging.Logger
import javax.net.ssl.SSLContext
import kotlin.text.Charsets.UTF_8

class IppConfig(

    // IPP config
    var userName: String? = System.getProperty("user.name"),
    var ippVersion: String = "2.0",
    var charset: Charset = UTF_8,
    var naturalLanguage: String = "en-us",

    // HTTP config
    var timeout: Duration = Duration.ofSeconds(30),
    var userAgent: String? = "ipp-client/3.2",
    var password: String? = null,
    var sslContext: SSLContext? = null,
    // trust any certificate: sslContextForAnyCertificate()
    // use individual certificate: sslContext(loadCertificate(FileInputStream("printer.pem")))
    // use truststore: sslContext(loadKeyStore(FileInputStream("printer.jks"), "changeit"))
    var verifySSLHostname: Boolean = true

) {
    fun authorization() =
        "Basic " + getEncoder().encodeToString("$userName:$password".toByteArray(UTF_8))

    fun trustAnyCertificateAndSSLHostname() {
        sslContext = SSLHelper.sslContextForAnyCertificate()
        verifySSLHostname = false
    }

    @JvmOverloads
    fun log(logger: Logger, level: Level = INFO) = logger.run {
        log(level) { "userName: $userName" }
        log(level) { "ippVersion: $ippVersion" }
        log(level) { "charset: ${charset.name().lowercase()}" }
        log(level) { "naturalLanguage: $naturalLanguage" }
        log(level) { "timeout: $timeout" }
        log(level) { "userAgent: $userAgent" }
        log(level) { "verifySSLHostname: $verifySSLHostname" }
    }
}