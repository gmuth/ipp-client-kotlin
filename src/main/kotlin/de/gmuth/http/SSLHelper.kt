package de.gmuth.http

/**
 * Copyright (c) 2020-2023 Gerhard Muth
 */

import java.io.InputStream
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

object SSLHelper {

    fun loadCertificate(inputStream: InputStream, type: String = "X.509") =
        CertificateFactory.getInstance(type).generateCertificate(inputStream)

    fun loadKeyStore(inputStream: InputStream, password: String, type: String = KeyStore.getDefaultType()) =
        KeyStore.getInstance(type).apply { load(inputStream, password.toCharArray()) }

    // to support old algorithms like SSLv3, change value of jdk.tls.disabledAlgorithms in java.security
    fun sslContext(trustmanagers: Array<TrustManager>, protocol: String = "TLS") =
        SSLContext.getInstance(protocol).apply { init(null, trustmanagers, SecureRandom()) }

    @SuppressWarnings("kotlin:S4830")
    fun sslContextForAnyCertificate() = sslContext(arrayOf(
        object : X509TrustManager {
            override fun checkClientTrusted(certificates: Array<out X509Certificate>?, string: String?) = Unit
            override fun checkServerTrusted(certificates: Array<out X509Certificate>?, string: String?) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
    ))

    fun sslContext(keyStore: KeyStore, algorithm: String = TrustManagerFactory.getDefaultAlgorithm()) =
        sslContext(TrustManagerFactory.getInstance(algorithm).apply { init(keyStore) }.trustManagers)

    fun sslContext(certificate: Certificate) = sslContext(
        KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null) // initialize keystore
            setCertificateEntry("alias", certificate)
        }
    )

}