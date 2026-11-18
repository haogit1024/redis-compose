package com.czh.rediscompose.feature.workspace

import com.czh.rediscompose.app.RedisSession
import com.czh.rediscompose.data.redis.HashFieldBytes
import com.czh.rediscompose.data.redis.RedisException
import com.czh.rediscompose.data.redis.RedisGateway
import com.czh.rediscompose.data.redis.SortedSetEntryBytes
import com.czh.rediscompose.data.redis.StreamEntryBytes
import com.czh.rediscompose.domain.command.ClientConnection
import com.czh.rediscompose.domain.command.CommandStat
import com.czh.rediscompose.domain.command.RedisReply
import com.czh.rediscompose.domain.command.SlowLogEntry
import com.czh.rediscompose.domain.connection.ConnectionProfile
import com.czh.rediscompose.domain.key.KeyType
import com.czh.rediscompose.domain.key.ScanPage
import com.czh.rediscompose.domain.settings.AppSettings
import com.czh.rediscompose.domain.settings.KeyView
import com.czh.rediscompose.domain.settings.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch

/**
 * Preview-only workspace fixture.
 *
 * It builds a real [WorkspaceState] on top of an in-memory gateway, so previews exercise
 * the same tree, filter and pin code as the app without touching a Redis server or the
 * user's connection files.
 */
internal fun previewWorkspaceState(
    empty: Boolean = false,
    readOnly: Boolean = false,
    paused: Boolean = false,
    scanError: Boolean = false,
    keyView: KeyView = KeyView.TREE,
): WorkspaceState {
    val scope = CoroutineScope(Dispatchers.Unconfined)
    val keys = if (empty) emptyList() else PREVIEW_KEYS
    val profile =
        ConnectionProfile(
            id = "preview-${if (empty) "empty" else "keys"}",
            name = "Preview",
            host = "127.0.0.1",
            readOnly = readOnly,
        )
    val gateway =
        PreviewKeyGateway(
            keys = keys,
            paused = paused,
            scanError = scanError,
        )
    val session = RedisSession(profile = profile, gateway = gateway, appScope = scope)
    scope.launch { session.connect() }
    val settings = AppSettings(theme = ThemeMode.DARK, showKeyTtl = true, keyView = keyView)
    return WorkspaceState(
        session = session,
        settings = { settings },
        onToast = { _, _ -> },
        scope = scope,
    ).also { workspace ->
        workspace.start()
        if (!empty && !paused && !scanError) {
            workspace.selectKey("cache:user:1001:profile")
        }
    }
}

private class PreviewKeyGateway(
    private val keys: List<String>,
    private val paused: Boolean,
    private val scanError: Boolean,
) : RedisGateway {
    override suspend fun ping(): String = "PONG"

    override suspend fun info(section: String?): String =
        if (section == "keyspace") {
            "# Keyspace\ndb0:keys=${keys.size},expires=3,avg_ttl=1000\n"
        } else {
            """
            # Server
            redis_version:7.4.2
            uptime_in_seconds:86400

            # Memory
            used_memory:1331439861
            maxmemory:2147483648

            # Clients
            connected_clients:24

            # Stats
            keyspace_hits:1000
            keyspace_misses:40
            total_commands_processed:90000
            instantaneous_ops_per_second:12.5

            # Replication
            role:master

            # Persistence
            rdb_last_save_time:1700000000
            aof_enabled:0

            # Keyspace
            db0:keys=${keys.size},expires=3,avg_ttl=1000
            """.trimIndent()
        }

    override suspend fun selectDatabase(index: Int) = Unit

    override suspend fun databaseSize(): Long = keys.size.toLong()

    override suspend fun scan(
        cursor: Long,
        pattern: String?,
        count: Int,
    ): ScanPage {
        if (paused) awaitCancellation()
        if (scanError) throw RedisException("连接已断开：Connection reset by peer")
        val matched =
            if (pattern.isNullOrBlank()) {
                keys
            } else {
                keys.filter { matchesPreviewPattern(it, pattern) }
            }
        return ScanPage(cursor = 0, keys = matched)
    }

    override suspend fun keyType(key: String): KeyType = previewType(key)

    override suspend fun keyTtl(key: String): Long = previewTtl(key)

    override suspend fun keyMemoryUsage(key: String): Long? = 1_024

    override suspend fun keyEncoding(key: String): String? = "embstr"

    override suspend fun getString(key: String): ByteArray? = null

    override suspend fun setString(
        key: String,
        value: ByteArray,
    ) = Unit

    override suspend fun getHash(key: String): List<HashFieldBytes> = emptyList()

    override suspend fun hashSet(
        key: String,
        field: String,
        value: ByteArray,
    ) = Unit

    override suspend fun hashDelete(
        key: String,
        field: String,
    ) = Unit

    override suspend fun getList(key: String): List<ByteArray> = emptyList()

    override suspend fun listPush(
        key: String,
        value: ByteArray,
        head: Boolean,
    ) = Unit

    override suspend fun listSet(
        key: String,
        index: Long,
        value: ByteArray,
    ) = Unit

    override suspend fun listRemove(
        key: String,
        value: ByteArray,
    ) = Unit

    override suspend fun getSet(key: String): List<ByteArray> = emptyList()

    override suspend fun setAdd(
        key: String,
        value: ByteArray,
    ) = Unit

    override suspend fun setRemove(
        key: String,
        value: ByteArray,
    ) = Unit

    override suspend fun getSortedSet(key: String): List<SortedSetEntryBytes> = emptyList()

    override suspend fun sortedSetAdd(
        key: String,
        member: ByteArray,
        score: Double,
    ) = Unit

    override suspend fun sortedSetRemove(
        key: String,
        member: ByteArray,
    ) = Unit

    override suspend fun getStream(
        key: String,
        count: Int,
    ): List<StreamEntryBytes> = emptyList()

    override suspend fun getStreamGroupCount(key: String): Int? = null

    override suspend fun deleteKeys(keys: List<String>) = Unit

    override suspend fun renameKey(
        oldKey: String,
        newKey: String,
    ) = Unit

    override suspend fun expire(
        key: String,
        seconds: Long,
    ) = Unit

    override suspend fun persist(key: String) = Unit

    override suspend fun slowLog(count: Int): List<SlowLogEntry> = emptyList()

    override suspend fun clients(): List<ClientConnection> = emptyList()

    override suspend fun commandStats(): List<CommandStat> = emptyList()

    override suspend fun execute(args: List<String>): RedisReply = RedisReply.Nil

    override fun close() = Unit
}

private fun previewType(key: String): KeyType =
    when {
        key.startsWith("app:session") ||
            key.contains(":profile") ||
            key.startsWith("billing:invoice") ||
            key.startsWith("metrics:daily") -> KeyType.HASH
        key.startsWith("queue:") -> KeyType.LIST
        key.startsWith("tags:") -> KeyType.SET
        key.startsWith("cache:search") ||
            key.startsWith("queue:jobs") ||
            key.startsWith("metrics:histogram") -> KeyType.ZSET
        key.startsWith("stream:") -> KeyType.STREAM
        else -> KeyType.STRING
    }

private fun previewTtl(key: String): Long =
    when {
        key.startsWith("temp:otp") -> 42
        key == "app:session:8f2a9c" -> 1_680
        key == "app:session:1c9de4" -> 7_200
        else -> -1
    }

private fun matchesPreviewPattern(
    key: String,
    pattern: String,
): Boolean {
    val regex =
        buildString {
            append('^')
            pattern.forEach { character ->
                when (character) {
                    '*' -> append(".*")
                    '?' -> append('.')
                    '.', '(', ')', '[', ']', '{', '}', '+', '^', '$', '|', '\\' -> append('\\').append(character)
                    else -> append(character)
                }
            }
            append('$')
        }
    return Regex(regex).matches(key)
}

private val PREVIEW_KEYS =
    listOf(
        "app:config:feature_flags",
        "app:config:rate_limits",
        "app:session:8f2a9c",
        "app:session:1c9de4",
        "cache:user:1001:profile",
        "cache:user:1001:settings",
        "cache:user:2048:profile",
        "cache:product:8842",
        "cache:product:9103",
        "cache:search:popular",
        "queue:emails",
        "queue:jobs:priority",
        "queue:dead_letter",
        "billing:invoice:INV-2026-1187",
        "billing:ledger:november",
        "metrics:daily:2026-11-17",
        "metrics:histogram:latency",
        "stream:events:audit",
        "tags:article:204",
        "locks:checkout:order-9182",
        "temp:otp:13800001111",
        "feature:rollout:checkout_v2",
    )
