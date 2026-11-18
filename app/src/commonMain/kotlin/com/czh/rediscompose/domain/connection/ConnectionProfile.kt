package com.czh.rediscompose.domain.connection

import kotlinx.serialization.Serializable

@Serializable
enum class ConnectionEnvironment { PRODUCTION, STAGING, LOCAL, ANALYTICS, OTHER }

@Serializable
enum class ConnectionColor { IRIS, MINT, AMBER, SKY, ROSE, LILAC }

@Serializable
enum class TlsVerifyMode { STRICT, ALLOW_SELF_SIGNED, NONE }

@Serializable
data class TlsConfig(
    val enabled: Boolean = false,
    val caPath: String = "",
    val clientCertPath: String = "",
    val clientKeyPath: String = "",
    val verifyMode: TlsVerifyMode = TlsVerifyMode.STRICT,
)

@Serializable
enum class SshAuthMethod { PRIVATE_KEY, PASSWORD }

@Serializable
data class SshConfig(
    val enabled: Boolean = false,
    val host: String = "",
    val port: Int = 22,
    val username: String = "",
    val authMethod: SshAuthMethod = SshAuthMethod.PRIVATE_KEY,
    val password: String = "",
    val privateKeyPath: String = "",
)

/**
 * Stored connection description. Runtime sessions live in `RedisSession` (Phase 3).
 *
 * Fields keep defaults so older `connections.json` files without them still load.
 */
@Serializable
data class ConnectionProfile(
    val id: String,
    val name: String,
    val environment: ConnectionEnvironment = ConnectionEnvironment.LOCAL,
    val host: String,
    val port: Int = 6379,
    val username: String = "",
    val password: String = "",
    val defaultDatabase: Int = 0,
    val keySeparator: String = ":",
    val color: ConnectionColor = ConnectionColor.IRIS,
    val group: String = "",
    val tls: TlsConfig = TlsConfig(),
    val ssh: SshConfig = SshConfig(),
    val readOnly: Boolean = false,
    val connectTimeoutMillis: Int = 5_000,
    val readTimeoutMillis: Int = 5_000,
    val lastOpenedAtEpochMillis: Long? = null,
    val createdAtEpochMillis: Long = 0,
    val updatedAtEpochMillis: Long = 0,
)
