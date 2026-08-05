package com.czh.redis.redis

enum class RedisType(val label: String) {
    STRING("STRING"),
    HASH("HASH"),
    LIST("LIST"),
    SET("SET"),
    ZSET("ZSET"),
    STREAM("STREAM"),
    NONE("NONE"),
    ;

    companion object {
        fun fromLabel(label: String?): RedisType = when (label?.uppercase()) {
            "STRING" -> STRING
            "HASH" -> HASH
            "LIST" -> LIST
            "SET" -> SET
            "ZSET" -> ZSET
            "STREAM" -> STREAM
            else -> NONE
        }
    }
}

data class ScanResult(val cursor: Long, val keys: List<String>)

data class DbInfo(val index: Int, val keyCount: Long)

data class ZSetEntry(val member: ByteArray, val score: Double)

sealed class CommandResult {
    data class Status(val value: String) : CommandResult()
    data class Integer(val value: Long) : CommandResult()
    data class Bulk(val value: ByteArray) : CommandResult()
    data class Array(val values: List<String>) : CommandResult()
    data class Error(val message: String) : CommandResult()
    object Ok : CommandResult()
    object Nil : CommandResult()
}

interface RedisClient {
    suspend fun ping(): String
    suspend fun info(section: String? = null): String
    suspend fun select(db: Int)
    suspend fun dbsize(): Long
    suspend fun scan(cursor: Long, pattern: String?, count: Int): ScanResult
    suspend fun type(key: String): RedisType
    suspend fun ttl(key: String): Long
    suspend fun memoryUsage(key: String): Long?
    suspend fun getString(key: String): ByteArray?
    suspend fun setString(key: String, value: ByteArray)
    suspend fun getHash(key: String): List<Pair<String, ByteArray>>
    suspend fun hSet(key: String, field: String, value: ByteArray)
    suspend fun hDel(key: String, field: String)
    suspend fun getList(key: String): List<ByteArray>
    suspend fun listPush(key: String, value: ByteArray, head: Boolean)
    suspend fun listSet(key: String, index: Long, value: ByteArray)
    suspend fun listRemove(key: String, value: ByteArray)
    suspend fun getSet(key: String): List<ByteArray>
    suspend fun setAdd(key: String, value: ByteArray)
    suspend fun setRemove(key: String, value: ByteArray)
    suspend fun getZSet(key: String): List<ZSetEntry>
    suspend fun zAdd(key: String, member: ByteArray, score: Double)
    suspend fun zRemove(key: String, member: ByteArray)
    suspend fun del(key: String)
    suspend fun rename(key: String, newKey: String)
    suspend fun expire(key: String, ttl: Long)
    suspend fun persist(key: String)
    suspend fun command(args: List<String>): CommandResult
    suspend fun close()
}
