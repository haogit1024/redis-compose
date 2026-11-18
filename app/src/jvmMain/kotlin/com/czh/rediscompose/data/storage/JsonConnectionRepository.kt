package com.czh.rediscompose.data.storage

import com.czh.rediscompose.domain.connection.ConnectionProfile
import com.czh.rediscompose.domain.connection.ConnectionRepository
import com.czh.rediscompose.domain.repository.PasswordCipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files

/**
 * Reads/writes the legacy bare-array `connections.json` format.
 *
 * Passwords are encrypted on save and decrypted on load; plaintext values from older
 * files pass through unchanged.
 *
 * TODO(phase-9): honor `AppSettings.encryptPasswords = false` (currently always encrypts).
 */
class JsonConnectionRepository(
    private val paths: FilePaths = FilePaths(),
    private val cipher: PasswordCipher,
    private val json: Json = storageJson,
) : ConnectionRepository {
    override suspend fun load(): List<ConnectionProfile> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!Files.exists(paths.connections)) return@runCatching emptyList()
                val text = Files.readString(paths.connections)
                if (text.isBlank()) return@runCatching emptyList()
                json.decodeFromString<List<ConnectionProfile>>(text).map { it.decrypted() }
            }.getOrElse { error ->
                println("[redis-compose] failed to read connections: ${error.firstLine()}")
                emptyList()
            }
        }

    override suspend fun save(connections: List<ConnectionProfile>) {
        withContext(Dispatchers.IO) {
            runCatching {
                val stored = connections.map { it.encrypted() }
                writeAtomically(paths.connections, json.encodeToString(stored))
            }.onFailure { error ->
                println("[redis-compose] failed to write connections: ${error.firstLine()}")
            }
        }
    }

    private fun ConnectionProfile.decrypted(): ConnectionProfile =
        copy(
            password = cipher.decrypt(password),
            ssh = ssh.copy(password = cipher.decrypt(ssh.password)),
        )

    private fun ConnectionProfile.encrypted(): ConnectionProfile =
        copy(
            password = cipher.encrypt(password),
            ssh = ssh.copy(password = cipher.encrypt(ssh.password)),
        )

    private fun Throwable.firstLine(): String = message?.lineSequence()?.firstOrNull() ?: this::class.simpleName.orEmpty()
}
