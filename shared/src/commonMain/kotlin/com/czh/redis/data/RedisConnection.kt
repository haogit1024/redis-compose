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
    val separator: String = "",
    val ssh: Boolean = false,
    val ssl: Boolean = false,
    val sentinel: Boolean = false,
    val cluster: Boolean = false,
    val readonly: Boolean = false,
) {
    @OptIn(ExperimentalUuidApi::class)
    fun withFreshId(): RedisConnection = copy(id = id.ifEmpty { Uuid.random().toString() })
}