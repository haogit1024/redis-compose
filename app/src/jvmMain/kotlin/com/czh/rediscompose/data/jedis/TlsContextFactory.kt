package com.czh.rediscompose.data.jedis

import com.czh.rediscompose.data.redis.RedisException
import com.czh.rediscompose.data.storage.expandUserPath
import com.czh.rediscompose.domain.connection.TlsConfig
import com.czh.rediscompose.domain.connection.TlsVerifyMode
import java.nio.file.Files
import java.security.KeyFactory
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import javax.net.ssl.KeyManager
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Builds an [SSLSocketFactory] for the TLS options in the connection form.
 *
 * - STRICT with no custom CA uses the system trust store.
 * - A custom CA replaces the system trust store.
 * - ALLOW_SELF_SIGNED / NONE without a CA use a trust-all manager (shown as amber in the UI).
 * - Client certificates require both a certificate and a PKCS#8 (or RSA PKCS#1) private key.
 */
internal object TlsContextFactory {
    fun socketFactory(config: TlsConfig): SSLSocketFactory? {
        val hasClientCertificate = config.clientCertPath.isNotBlank() || config.clientKeyPath.isNotBlank()
        if (hasClientCertificate && (config.clientCertPath.isBlank() || config.clientKeyPath.isBlank())) {
            throw RedisException("客户端证书与私钥必须同时提供")
        }
        val customTrust = config.caPath.isNotBlank() || config.verifyMode != TlsVerifyMode.STRICT
        if (!hasClientCertificate && !customTrust) return null

        val keyManagers =
            if (hasClientCertificate) {
                keyManagers(config.clientCertPath, config.clientKeyPath)
            } else {
                null
            }
        val trustManagers = trustManagers(config)
        return try {
            SSLContext.getInstance("TLS").apply {
                init(keyManagers, trustManagers, SecureRandom())
            }.socketFactory
        } catch (error: Exception) {
            throw RedisException("初始化 TLS 失败：${error.firstLine()}", error)
        }
    }

    private fun trustManagers(config: TlsConfig): Array<TrustManager>? =
        when {
            config.verifyMode == TlsVerifyMode.NONE -> arrayOf(trustAllManager())
            config.caPath.isNotBlank() -> trustManagersForCa(config.caPath)
            config.verifyMode == TlsVerifyMode.ALLOW_SELF_SIGNED -> arrayOf(trustAllManager())
            else -> null
        }

    private fun trustManagersForCa(path: String): Array<TrustManager> =
        runCatching {
            val certificates =
                Files.newInputStream(expandUserPath(path)).use { stream ->
                    CertificateFactory.getInstance("X.509").generateCertificates(stream)
                }
            if (certificates.isEmpty()) error("empty certificate file")
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply { load(null, null) }
            certificates.forEachIndexed { index, certificate ->
                keyStore.setCertificateEntry("ca-$index", certificate)
            }
            val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            factory.init(keyStore)
            factory.trustManagers
        }.getOrElse { error ->
            throw RedisException("无法读取自定义 CA 证书：${error.firstLine()}", error)
        }

    private fun keyManagers(
        certificatePath: String,
        privateKeyPath: String,
    ): Array<KeyManager> =
        runCatching {
            val chain = readCertificates(certificatePath)
            val privateKey = readPrivateKey(privateKeyPath)
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply { load(null, null) }
            keyStore.setKeyEntry("redis-compose-client", privateKey, charArrayOf(), chain)
            val factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            factory.init(keyStore, charArrayOf())
            factory.keyManagers
        }.getOrElse { error ->
            if (error is RedisException) throw error
            throw RedisException("无法加载客户端证书：${error.firstLine()}", error)
        }

    private fun readCertificates(path: String): Array<Certificate> =
        Files.newInputStream(expandUserPath(path)).use { stream ->
            val certificates = CertificateFactory.getInstance("X.509").generateCertificates(stream).toTypedArray()
            if (certificates.isEmpty()) throw RedisException("客户端证书文件为空")
            certificates
        }

    private fun readPrivateKey(path: String): PrivateKey {
        val text =
            runCatching { Files.readString(expandUserPath(path)) }
                .getOrElse { throw RedisException("无法读取客户端私钥：${it.firstLine()}", it) }
        if (text.contains("ENCRYPTED") || text.contains("Proc-Type: 4,ENCRYPTED")) {
            throw RedisException("暂不支持加密的客户端私钥，请提供未加密的 PKCS#8 / RSA 私钥")
        }
        val pem =
            when {
                text.contains("-----BEGIN PRIVATE KEY-----") -> pemBody(text, "PRIVATE KEY")
                text.contains("-----BEGIN RSA PRIVATE KEY-----") -> wrapRsaPkcs1(pemBody(text, "RSA PRIVATE KEY"))
                else -> throw RedisException("无法识别的私钥格式，请使用 PKCS#8 或 RSA PEM")
            }
        val spec = PKCS8EncodedKeySpec(pem)
        val key =
            listOf("RSA", "EC", "Ed25519").firstNotNullOfOrNull { algorithm ->
                runCatching { KeyFactory.getInstance(algorithm).generatePrivate(spec) }.getOrNull()
            }
        return key ?: throw RedisException("无法解析客户端私钥，请确认为未加密的 PKCS#8 / RSA 私钥")
    }

    private fun pemBody(
        text: String,
        label: String,
    ): ByteArray {
        val marker = "-----BEGIN $label-----"
        val endMarker = "-----END $label-----"
        val start = text.indexOf(marker)
        val end = text.indexOf(endMarker)
        if (start < 0 || end <= start) throw RedisException("私钥 PEM 头尾不完整")
        val body = text.substring(start + marker.length, end).filterNot { it.isWhitespace() }
        return runCatching { Base64.getDecoder().decode(body) }
            .getOrElse { throw RedisException("私钥 Base64 解码失败") }
    }

    private fun wrapRsaPkcs1(pkcs1: ByteArray): ByteArray {
        val version = byteArrayOf(0x02, 0x01, 0x00)
        val algorithm = RSA_ALGORITHM_IDENTIFIER
        val octet = der(0x04, pkcs1)
        return der(0x30, version + algorithm + octet)
    }

    private fun der(
        tag: Int,
        content: ByteArray,
    ): ByteArray {
        val length = content.size
        val encodedLength =
            when {
                length < 0x80 -> byteArrayOf(length.toByte())
                length <= 0xFF -> byteArrayOf(0x81.toByte(), length.toByte())
                length <= 0xFFFF ->
                    byteArrayOf(
                        0x82.toByte(),
                        (length ushr 8).toByte(),
                        length.toByte(),
                    )
                else ->
                    byteArrayOf(
                        0x83.toByte(),
                        (length ushr 16).toByte(),
                        (length ushr 8).toByte(),
                        length.toByte(),
                    )
            }
        return byteArrayOf(tag.toByte()) + encodedLength + content
    }

    private fun trustAllManager(): X509TrustManager =
        object : X509TrustManager {
            override fun checkClientTrusted(
                chain: Array<out X509Certificate>?,
                authType: String?,
            ) = Unit

            override fun checkServerTrusted(
                chain: Array<out X509Certificate>?,
                authType: String?,
            ) = Unit

            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }

    private fun Throwable.firstLine(): String =
        message?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() }
            ?: this::class.simpleName
            ?: "未知错误"

    private val RSA_ALGORITHM_IDENTIFIER =
        byteArrayOf(
            0x30,
            0x0D,
            0x06,
            0x09,
            0x2A,
            0x86.toByte(),
            0x48,
            0x86.toByte(),
            0xF7.toByte(),
            0x0D,
            0x01,
            0x01,
            0x01,
            0x05,
            0x00,
        )
}
