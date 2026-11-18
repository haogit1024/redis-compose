package com.czh.rediscompose.data.storage

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AesGcmPasswordCipherTest {
    private fun paths(): FilePaths = FilePaths(Files.createTempDirectory("redis-compose-cipher-"))

    @Test
    fun `encrypt decrypt round trip uses the v1 prefix`() {
        val paths = paths()
        val cipher = AesGcmPasswordCipher(paths)
        val encrypted = cipher.encrypt("redis-secret")

        assertTrue(encrypted.startsWith("enc:v1:"))
        assertFalse(encrypted.contains("redis-secret"))
        assertEquals("redis-secret", cipher.decrypt(encrypted))
        assertEquals(32, Files.readAllBytes(paths.secretKey).size)
    }

    @Test
    fun `each encryption uses a fresh iv`() {
        val cipher = AesGcmPasswordCipher(paths())
        assertTrue(cipher.encrypt("secret") != cipher.encrypt("secret"))
    }

    @Test
    fun `empty and legacy plaintext values pass through`() {
        val cipher = AesGcmPasswordCipher(paths())
        assertEquals("", cipher.encrypt(""))
        assertEquals("", cipher.decrypt(""))
        assertEquals("plain", cipher.decrypt("plain"))
    }

    @Test
    fun `already encrypted values are not encrypted twice`() {
        val cipher = AesGcmPasswordCipher(paths())
        val encrypted = cipher.encrypt("secret")
        assertEquals(encrypted, cipher.encrypt(encrypted))
    }

    @Test
    fun `invalid ciphertext decrypts to empty instead of throwing`() {
        val cipher = AesGcmPasswordCipher(paths())
        assertEquals("", cipher.decrypt("enc:v1:not-base64"))
        assertEquals("", cipher.decrypt("enc:v1:AAAA"))
    }

    @Test
    fun `key survives a new cipher instance`() {
        val paths = paths()
        val encrypted = AesGcmPasswordCipher(paths).encrypt("secret")
        assertEquals("secret", AesGcmPasswordCipher(paths).decrypt(encrypted))
    }

    @Test
    fun `corrupted key file falls back to plaintext`() {
        val paths = paths()
        Files.createDirectories(paths.baseDir)
        Files.write(paths.secretKey, byteArrayOf(1, 2, 3))
        val cipher = AesGcmPasswordCipher(paths)
        assertEquals("plain", cipher.encrypt("plain"))
        assertEquals("", cipher.decrypt("enc:v1:AAAA"))
    }
}
