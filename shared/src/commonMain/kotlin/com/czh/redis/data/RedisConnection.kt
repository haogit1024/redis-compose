package com.czh.redis.data

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class RedisConnection(
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
    val sentinel: Boolean = false,
    val sentinelMaster: String = "",
    val cluster: Boolean = false,
    val readonly: Boolean = false,
    val connectTimeoutMs: Long = 5000,
    val readTimeoutMs: Long = 5000,
) {
    @OptIn(ExperimentalUuidApi::class)
    fun withFreshId(): RedisConnection = copy(id = id.ifEmpty { Uuid.random().toString() })

    fun validate(): String? = when {
        name.isBlank() -> "连接名称不能为空"
        host.isBlank() -> "Host 不能为空"
        port.toIntOrNull() == null -> "端口格式不正确"
        else -> null
    }
}
