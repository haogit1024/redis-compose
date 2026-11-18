package com.czh.rediscompose.domain.repository

/**
 * Encrypts secrets before they reach `connections.json`.
 *
 * Implementations must treat values without the `enc:` prefix as legacy plaintext on
 * [decrypt], so existing user files keep working.
 */
interface PasswordCipher {
    fun encrypt(plain: String): String

    fun decrypt(stored: String): String
}
