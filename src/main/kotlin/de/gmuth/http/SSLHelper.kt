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
import javax.net.ssl.*

class SSLHelper {

    companion object {

        fun loadCertificate(inputStream: InputStream, type: String = "X.509"): Certificate {
            return CertificateFactory.getInstance(type).generateCertificate(inputStream)
        }

        fun loadTrustStore(keyStoreInputStream: InputStream, keyStorePassword: String, keyStoreType: String = KeyStore.getDefaultType()): KeyStore {
            return KeyStore.getInstance(keyStoreType).apply {
                load(keyStoreInputStream, keyStorePassword.toCharArray())
            }
        }

        fun sslSocketFactoryForAnyCertificate(): SSLSocketFactory {
            val anyCertificateX509TrustManager = object : X509TrustManager {
                override fun checkClientTrusted(certificates: Array<out X509Certificate>?, string: String?) = Unit
                override fun checkServerTrusted(certificates: Array<out X509Certificate>?, string: String?) = Unit
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
            return sslSocketFactory(arrayOf<TrustManager>(anyCertificateX509TrustManager))
        }

        fun sslSocketFactory(certificate: Certificate): SSLSocketFactory {
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                load(null) // initialize keystore
                setCertificateEntry("alias", certificate)
            }
            return sslSocketFactory(keyStore)
        }


        fun sslSocketFactory(keyStore: KeyStore, algorithm: String = TrustManagerFactory.getDefaultAlgorithm()): SSLSocketFactory {
            val trustManagerFactory = TrustManagerFactory.getInstance(algorithm).apply {
                init(keyStore)
            }
            return sslSocketFactory(trustManagerFactory.trustManagers)
        }

        private fun sslSocketFactory(trustmanagers: Array<TrustManager>, protocol: String = "TLS"): SSLSocketFactory {
            val sslContext = SSLContext.getInstance(protocol).apply {
                init(null, trustmanagers, SecureRandom())
            }
            return sslContext.socketFactory
        }

    }

}