package com.example.yourfriend.chat

import android.content.Context
import com.example.yourfriend.R
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * GigaChat использует сертификаты НУЦ Минцифры, которых нет в системном
 * хранилище Android. Добавляем корневой и выпускающий сертификаты в отдельный
 * trust store только для запросов к GigaChat.
 */
object GigaChatTls {

    fun create(context: Context): Pair<SSLSocketFactory, X509TrustManager> {
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
        }
        val certificateFactory = CertificateFactory.getInstance("X.509")

        listOf(
            R.raw.russian_trusted_root_ca to "russian_trusted_root_ca",
            R.raw.russian_trusted_sub_ca to "russian_trusted_sub_ca"
        ).forEach { (resourceId, alias) ->
            context.resources.openRawResource(resourceId).use { input ->
                keyStore.setCertificateEntry(alias, certificateFactory.generateCertificate(input))
            }
        }

        val trustManagerFactory = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm()
        )
        trustManagerFactory.init(keyStore)
        val trustManager = trustManagerFactory.trustManagers
            .filterIsInstance<X509TrustManager>()
            .firstOrNull()
            ?: error("Не найден X509TrustManager")

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustManager), SecureRandom())
        return sslContext.socketFactory to trustManager
    }
}
