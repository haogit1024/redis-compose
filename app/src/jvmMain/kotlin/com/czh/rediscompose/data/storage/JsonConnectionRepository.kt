package com.czh.rediscompose.data.storage

import com.czh.rediscompose.domain.connection.ConnectionColor
import com.czh.rediscompose.domain.connection.ConnectionEnvironment
import com.czh.rediscompose.domain.connection.ConnectionProfile
import com.czh.rediscompose.domain.connection.ConnectionRepository
import com.czh.rediscompose.domain.connection.SshAuthMethod
import com.czh.rediscompose.domain.connection.SshConfig
import com.czh.rediscompose.domain.connection.TlsConfig
import com.czh.rediscompose.domain.repository.PasswordCipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
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
                decodeConnections(text).map { it.decrypted() }
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

    private fun decodeConnections(text: String): List<ConnectionProfile> {
        val modern = runCatching { json.decodeFromString<List<ConnectionProfile>>(text) }.getOrNull()
        if (modern != null) return modern
        return runCatching { json.decodeFromString<List<LegacyConnection>>(text).map { it.toProfile() } }
            .getOrElse { emptyList() }
    }
}

/**
 * Connection entry written by the pre-rewrite app.
 *
 * Ports are strings there and SSH/TLS are flat booleans + fields; mapping them keeps
 * existing `connections.json` usable without asking the user to re-enter servers.
 */
@Serializable
private data class LegacyConnection(
    val id: String = "",
    val name: String = "",
    val host: String = "",
    val port: String = "6379",
    val username: String = "",
    val password: String = "",
    val defaultDb: Int = 0,
    val separator: String = ":",
    val color: String = "",
    val group: String = "",
    val ssh: Boolean = false,
    val sshHost: String = "",
    val sshPort: String = "22",
    val sshUsername: String = "",
    val sshPassword: String = "",
    val sshPrivateKey: String = "",
    val ssl: Boolean = false,
    val sslCa: String = "",
    val sslCert: String = "",
    val sslKey: String = "",
    val readonly: Boolean = false,
    val connectTimeoutMs: Long = 5_000,
    val readTimeoutMs: Long = 5_000,
) {
    fun toProfile(): ConnectionProfile {
        val redisPort = port.toIntOrNull() ?: 6379
        return ConnectionProfile(
            id = id.ifBlank { "legacy-$host-$redisPort" },
            name = name.ifBlank { host.ifBlank { "Redis" } },
            environment = environmentFromGroup(group),
            host = host,
            port = redisPort,
            username = username,
            password = password,
            defaultDatabase = defaultDb,
            keySeparator = separator.ifEmpty { ":" },
            color = colorFromString(color),
            group = group,
            tls =
                TlsConfig(
                    enabled = ssl,
                    caPath = sslCa,
                    clientCertPath = sslCert,
                    clientKeyPath = sslKey,
                ),
            ssh =
                SshConfig(
                    enabled = ssh,
                    host = sshHost,
                    port = sshPort.toIntOrNull() ?: 22,
                    username = sshUsername,
                    authMethod = if (sshPrivateKey.isNotBlank()) SshAuthMethod.PRIVATE_KEY else SshAuthMethod.PASSWORD,
                    password = sshPassword,
                    privateKeyPath = sshPrivateKey,
                ),
            readOnly = readonly,
            connectTimeoutMillis = connectTimeoutMs.coerceIn(1, Int.MAX_VALUE.toLong()).toInt(),
            readTimeoutMillis = readTimeoutMs.coerceIn(1, Int.MAX_VALUE.toLong()).toInt(),
        )
    }

    private fun environmentFromGroup(group: String): ConnectionEnvironment =
        when (group.lowercase()) {
            "production", "prod" -> ConnectionEnvironment.PRODUCTION
            "staging" -> ConnectionEnvironment.STAGING
            "local" -> ConnectionEnvironment.LOCAL
            "analytics" -> ConnectionEnvironment.ANALYTICS
            else -> if (group.isBlank()) ConnectionEnvironment.LOCAL else ConnectionEnvironment.OTHER
        }

    private fun colorFromString(color: String): ConnectionColor =
        when (color.lowercase()) {
            "mint" -> ConnectionColor.MINT
            "amber" -> ConnectionColor.AMBER
            "sky" -> ConnectionColor.SKY
            "rose" -> ConnectionColor.ROSE
            "lilac" -> ConnectionColor.LILAC
            else -> ConnectionColor.IRIS
        }
}
