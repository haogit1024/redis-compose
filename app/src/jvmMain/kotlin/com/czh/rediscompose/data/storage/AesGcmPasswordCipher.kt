package com.czh.rediscompose.data.storage

import com.czh.rediscompose.domain.repository.PasswordCipher
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermissions
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM cipher for stored connection passwords.
 *
 * Output format is `enc:v1:<base64(iv + ciphertext)>`. The 32-byte key lives in
 * `secret.key` with owner-only permissions on POSIX. If the key cannot be read, the
 * cipher falls back to plaintext for the current session instead of losing credentials.
 */
class AesGcmPasswordCipher(
    private val paths: FilePaths = FilePaths(),
) : PasswordCipher {
    private val key: SecretKey? by lazy { loadOrCreateKey() }

    override fun encrypt(plain: String): String {
        if (plain.isEmpty()) return ""
        if (plain.startsWith(PREFIX)) return plain
        val secretKey = key ?: return plain
        return runCatching {
            val iv = ByteArray(IV_SIZE).also { SecureRandom().nextBytes(it) }
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_BITS, iv))
            val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            PREFIX + Base64.getEncoder().encodeToString(iv + encrypted)
        }.getOrElse { error ->
            println("[redis-compose] password encryption failed: ${error.firstLine()}")
            plain
        }
    }

    override fun decrypt(stored: String): String {
        if (!stored.startsWith(PREFIX)) return stored
        val secretKey = key ?: return ""
        return runCatching {
            val payload = Base64.getDecoder().decode(stored.removePrefix(PREFIX))
            if (payload.size <= IV_SIZE) return@runCatching ""
            val iv = payload.copyOfRange(0, IV_SIZE)
            val encrypted = payload.copyOfRange(IV_SIZE, payload.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_BITS, iv))
            cipher.doFinal(encrypted).toString(Charsets.UTF_8)
        }.getOrElse {
            ""
        }
    }

    private fun loadOrCreateKey(): SecretKey? =
        synchronized(this) {
            runCatching {
                Files.createDirectories(paths.baseDir)
                if (Files.exists(paths.secretKey)) {
                    val existing = Files.readAllBytes(paths.secretKey)
                    if (existing.size != KEY_SIZE) {
                        println("[redis-compose] secret.key has an unexpected size; passwords are stored as plaintext")
                        null
                    } else {
                        SecretKeySpec(existing, KEY_ALGORITHM)
                    }
                } else {
                    val generated = ByteArray(KEY_SIZE).also { SecureRandom().nextBytes(it) }
                    Files.write(paths.secretKey, generated, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
                    runCatching {
                        Files.setPosixFilePermissions(
                            paths.secretKey,
                            PosixFilePermissions.fromString("rw-------"),
                        )
                    }
                    SecretKeySpec(generated, KEY_ALGORITHM)
                }
            }.getOrElse { error ->
                println("[redis-compose] secret.key unavailable: ${error.firstLine()}")
                null
            }
        }

    private fun Throwable.firstLine(): String = message?.lineSequence()?.firstOrNull() ?: this::class.simpleName.orEmpty()

    private companion object {
        const val PREFIX = "enc:v1:"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_ALGORITHM = "AES"
        const val IV_SIZE = 12
        const val TAG_BITS = 128
        const val KEY_SIZE = 32
    }
}
