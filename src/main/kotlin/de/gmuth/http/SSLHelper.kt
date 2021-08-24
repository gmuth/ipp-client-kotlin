package de.gmuth.http

/**
 * Copyright (c) 2020 Gerhard Muth
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

    fun loadKeyStore(keyStoreInputStream: InputStream, keyStorePassword: String, keyStoreType: String = KeyStore.getDefaultType()) =
            KeyStore.getInstance(keyStoreType).apply { load(keyStoreInputStream, keyStorePassword.toCharArray()) }

    fun sslSocketFactory(trustmanagers: Array<TrustManager>, protocol: String = "TLS") =
            SSLContext.getInstance(protocol).apply { init(null, trustmanagers, SecureRandom()) }.socketFactory

    @SuppressWarnings("kotlin:S4830")
    fun sslSocketFactoryForAnyCertificate() = sslSocketFactory(arrayOf(
            object : X509TrustManager {
                override fun checkClientTrusted(certificates: Array<out X509Certificate>?, string: String?) = Unit
                override fun checkServerTrusted(certificates: Array<out X509Certificate>?, string: String?) = Unit
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
    ))

    fun sslSocketFactory(certificate: Certificate) = sslSocketFactory(
            KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                load(null) // initialize keystore
                setCertificateEntry("alias", certificate)
            }
    )

    fun sslSocketFactory(keyStore: KeyStore, algorithm: String = TrustManagerFactory.getDefaultAlgorithm()) = sslSocketFactory(
            TrustManagerFactory.getInstance(algorithm).apply { init(keyStore) }.trustManagers
    )

}