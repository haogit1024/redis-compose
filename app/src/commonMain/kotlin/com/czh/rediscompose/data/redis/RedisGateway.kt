package com.czh.rediscompose.data.redis

import com.czh.rediscompose.domain.command.ClientConnection
import com.czh.rediscompose.domain.command.CommandStat
import com.czh.rediscompose.domain.command.RedisReply
import com.czh.rediscompose.domain.command.SlowLogEntry
import com.czh.rediscompose.domain.key.KeyType
import com.czh.rediscompose.domain.key.ScanPage

data class HashFieldBytes(
    val field: String,
    val value: ByteArray,
)

data class SortedSetEntryBytes(
    val member: ByteArray,
    val score: Double,
)

data class StreamEntryBytes(
    val id: String,
    val fields: List<Pair<String, ByteArray>>,
)

/**
 * All Redis access goes through this interface; feature/UI code never imports Jedis.
 *
 * Implementations are expected to serialize operations on the same connection and run
 * them off the main thread.
 */
interface RedisGateway {
    suspend fun ping(): String

    suspend fun info(section: String? = null): String

    suspend fun selectDatabase(index: Int)

    suspend fun databaseSize(): Long

    suspend fun scan(
        cursor: Long,
        pattern: String?,
        count: Int,
    ): ScanPage

    suspend fun keyType(key: String): KeyType

    suspend fun keyTtl(key: String): Long

    suspend fun keyMemoryUsage(key: String): Long?

    suspend fun keyEncoding(key: String): String?

    suspend fun getString(key: String): ByteArray?

    suspend fun setString(
        key: String,
        value: ByteArray,
    )

    suspend fun getHash(key: String): List<HashFieldBytes>

    suspend fun hashSet(
        key: String,
        field: String,
        value: ByteArray,
    )

    suspend fun hashDelete(
        key: String,
        field: String,
    )

    suspend fun getList(key: String): List<ByteArray>

    suspend fun listPush(
        key: String,
        value: ByteArray,
        head: Boolean,
    )

    suspend fun listSet(
        key: String,
        index: Long,
        value: ByteArray,
    )

    suspend fun listRemove(
        key: String,
        value: ByteArray,
    )

    suspend fun getSet(key: String): List<ByteArray>

    suspend fun setAdd(
        key: String,
        value: ByteArray,
    )

    suspend fun setRemove(
        key: String,
        value: ByteArray,
    )

    suspend fun getSortedSet(key: String): List<SortedSetEntryBytes>

    suspend fun sortedSetAdd(
        key: String,
        member: ByteArray,
        score: Double,
    )

    suspend fun sortedSetRemove(
        key: String,
        member: ByteArray,
    )

    suspend fun getStream(
        key: String,
        count: Int = 50,
    ): List<StreamEntryBytes>

    suspend fun getStreamGroupCount(key: String): Int?

    suspend fun deleteKeys(keys: List<String>)

    suspend fun renameKey(
        oldKey: String,
        newKey: String,
    )

    suspend fun expire(
        key: String,
        seconds: Long,
    )

    suspend fun persist(key: String)

    suspend fun slowLog(count: Int = 10): List<SlowLogEntry>

    suspend fun clients(): List<ClientConnection>

    suspend fun commandStats(): List<CommandStat>

    suspend fun execute(args: List<String>): RedisReply

    /** Releases the connection. Safe to call from the main thread. */
    fun close()
}
