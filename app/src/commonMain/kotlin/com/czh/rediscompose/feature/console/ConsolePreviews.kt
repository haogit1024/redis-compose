package com.czh.rediscompose.feature.console

import com.czh.rediscompose.app.RedisSession
import com.czh.rediscompose.data.redis.HashFieldBytes
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
import com.czh.rediscompose.domain.settings.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Preview-only console fixture.
 *
 * Builds a real [ConsoleState] over an in-memory gateway so previews exercise the same
 * rendering, history and suggestion paths without opening a connection.
 */
internal fun previewConsoleState(readOnly: Boolean = false): ConsoleState {
    val scope = CoroutineScope(Dispatchers.Unconfined)
    val profile =
        ConnectionProfile(
            id = "preview-console",
            name = "Local Sandbox",
            host = "127.0.0.1",
            port = 6379,
            readOnly = readOnly,
        )
    val session = RedisSession(profile = profile, gateway = PreviewConsoleGateway(), appScope = scope)
    scope.launch { session.connect() }
    val settings = AppSettings(theme = ThemeMode.DARK)
    val console =
        ConsoleState(
            session = session,
            settings = { settings },
            onToast = { _, _ -> },
            onRecordCommand = {},
            scope = scope,
        )
    listOf(
        "HGETALL cache:user:1001:profile",
        "TTL cache:user:1001:profile",
        "SCAN 0 MATCH queue:* COUNT 100",
        "SLOWLOG GET 10",
        "INFO memory",
    ).forEach { command -> console.execute(command) }
    return console
}

private class PreviewConsoleGateway : RedisGateway {
    override suspend fun ping(): String = "PONG"

    override suspend fun info(section: String?): String =
        if (section == "keyspace") {
            "# Keyspace\ndb0:keys=12480,expires=30,avg_ttl=1000\n"
        } else {
            """
            # Server
            redis_version:8.6.2
            uptime_in_seconds:86400

            # Memory
            used_memory:1331439861
            maxmemory:2147483648

            # Clients
            connected_clients:24

            # Keyspace
            db0:keys=12480,expires=30,avg_ttl=1000
            """.trimIndent()
        }

    override suspend fun databaseCount(): Int? = 16

    override suspend fun selectDatabase(index: Int) = Unit

    override suspend fun databaseSize(): Long = 12_480

    override suspend fun scan(
        cursor: Long,
        pattern: String?,
        count: Int,
    ): ScanPage = ScanPage(0, emptyList())

    override suspend fun keyType(key: String): KeyType = KeyType.STRING

    override suspend fun keyTtl(key: String): Long = -1

    override suspend fun keyMemoryUsage(key: String): Long? = null

    override suspend fun keyEncoding(key: String): String? = null

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

    override suspend fun execute(args: List<String>): RedisReply {
        val command = args.firstOrNull()?.uppercase() ?: return RedisReply.Error("ERR empty command")
        return when (command) {
            "PING" -> RedisReply.Simple("PONG")
            "DBSIZE" -> RedisReply.Integer(12_480)
            "TTL" -> RedisReply.Integer(3_271)
            "GET" -> RedisReply.Bulk("482913".encodeToByteArray())
            "HGETALL" ->
                RedisReply.Array(
                    listOf(
                        "name" to "Ada Lovelace",
                        "email" to "ada@acme.io",
                        "plan" to "scale",
                        "company" to "Analytical Engines Ltd.",
                        "region" to "ap-east-1",
                    ).flatMap { (field, value) ->
                        listOf(
                            RedisReply.Bulk(field.encodeToByteArray()),
                            RedisReply.Bulk(value.encodeToByteArray()),
                        )
                    },
                )
            "SCAN" ->
                RedisReply.Array(
                    listOf(
                        RedisReply.Bulk("17".encodeToByteArray()),
                        RedisReply.Array(
                            listOf(
                                RedisReply.Bulk("queue:emails".encodeToByteArray()),
                                RedisReply.Bulk("queue:jobs:priority".encodeToByteArray()),
                                RedisReply.Bulk("queue:dead_letter".encodeToByteArray()),
                            ),
                        ),
                    ),
                )
            "SLOWLOG" ->
                RedisReply.Array(
                    listOf(
                        RedisReply.Array(
                            listOf(
                                RedisReply.Integer(4_218),
                                RedisReply.Integer(1_731_905_984),
                                RedisReply.Integer(18_420),
                                RedisReply.Array(
                                    listOf(
                                        RedisReply.Bulk("GET".encodeToByteArray()),
                                        RedisReply.Bulk("cache:product:8842".encodeToByteArray()),
                                    ),
                                ),
                            ),
                        ),
                        RedisReply.Array(
                            listOf(
                                RedisReply.Integer(4_217),
                                RedisReply.Integer(1_731_905_902),
                                RedisReply.Integer(12_044),
                                RedisReply.Array(
                                    listOf(
                                        RedisReply.Bulk("HGETALL".encodeToByteArray()),
                                        RedisReply.Bulk("cache:user:1001:profile".encodeToByteArray()),
                                    ),
                                ),
                            ),
                        ),
                    ),
                )
            "INFO" ->
                RedisReply.Bulk(
                    """
                    # Memory
                    used_memory:1334234112
                    used_memory_human:1.24G
                    used_memory_peak:1588510720
                    mem_fragmentation_ratio:1.11
                    maxmemory_policy:allkeys-lru
                    """.trimIndent().encodeToByteArray(),
                )
            "MEMORY" -> RedisReply.Integer(1_432)
            "CLIENT" -> RedisReply.Bulk("id=18 addr=10.0.4.6:52412 name=redis-compose db=0\n".encodeToByteArray())
            else -> RedisReply.Error("ERR unknown command '${args.first()}'")
        }
    }

    override fun close() = Unit
}
