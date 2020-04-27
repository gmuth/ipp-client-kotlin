package de.gmuth.http

/**
 * Copyright (c) 2020 Gerhard Muth
 */

import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

class AnyCertificateX509TrustManager : X509TrustManager {

    @Throws(CertificateException::class)
    override fun checkClientTrusted(certificates: Array<X509Certificate?>?, string: String?) = Unit

    @Throws(CertificateException::class)
    override fun checkServerTrusted(certificates: Array<X509Certificate?>?, string: String?) = Unit

    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()

    companion object {
        // SSLContext is not thread-safe, so create a new SSLContext each time
        fun getNewSSLContextInstance(): SSLContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(AnyCertificateX509TrustManager()), SecureRandom())
        }

        val socketFactory: SSLSocketFactory = getNewSSLContextInstance().socketFactory
    }

}