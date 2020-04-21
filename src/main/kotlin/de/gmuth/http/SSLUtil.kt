package de.gmuth.http

import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class SSLUtil {
    companion object {
        val trustAllSSLContext = SSLContext.getInstance("TLS").apply {
            val trustAnyCertificate: Array<TrustManager> = arrayOf(object : X509TrustManager {
                @Throws(CertificateException::class)
                override fun checkClientTrusted(certificates: Array<X509Certificate?>?, string: String?) {
                }

                @Throws(CertificateException::class)
                override fun checkServerTrusted(certificates: Array<X509Certificate?>?, string: String?) {
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })
            init(null, trustAnyCertificate, SecureRandom())
        }
    }
}