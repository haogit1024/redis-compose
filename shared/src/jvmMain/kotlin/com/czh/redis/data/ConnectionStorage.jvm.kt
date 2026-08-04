package com.czh.redis.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private val storageFile: File
    get() = File(System.getProperty("user.home"), ".redis-compose/connections.json")

private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

actual fun loadConnections(): List<RedisConnection> {
    val file = storageFile
    if (!file.exists()) return emptyList()
    return runCatching {
        json.decodeFromString(connectionListSerializer, file.readText())
    }.getOrDefault(emptyList())
}

actual fun saveConnections(connections: List<RedisConnection>) {
    val file = storageFile
    runCatching {
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(connectionListSerializer, connections))
    }
}