package com.czh.rediscompose.app

import com.czh.rediscompose.data.redis.HashFieldBytes
import com.czh.rediscompose.data.redis.RedisGateway
import com.czh.rediscompose.data.redis.RedisGatewayFactory
import com.czh.rediscompose.data.redis.SortedSetEntryBytes
import com.czh.rediscompose.data.redis.StreamEntryBytes
import com.czh.rediscompose.domain.command.ClientConnection
import com.czh.rediscompose.domain.command.CommandStat
import com.czh.rediscompose.domain.command.RedisReply
import com.czh.rediscompose.domain.command.SlowLogEntry
import com.czh.rediscompose.domain.connection.ConnectionProfile
import com.czh.rediscompose.domain.connection.ConnectionRepository
import com.czh.rediscompose.domain.key.KeyType
import com.czh.rediscompose.domain.key.ScanPage
import com.czh.rediscompose.domain.repository.PasswordCipher
import com.czh.rediscompose.domain.settings.AppSettings
import com.czh.rediscompose.domain.settings.SettingsRepository

/**
 * In-memory backend for AppState tests and future feature tests.
 */
class FakeRedisGateway(
    var pingError: Exception? = null,
    var infoError: Exception? = null,
) : RedisGateway {
    var connected = false
    var selectedDatabase = 0
    var pingCount = 0

    var infoRaw: String = DEFAULT_INFO
    var keyspaceRaw: String = DEFAULT_KEYSPACE

    override suspend fun ping(): String {
        pingCount += 1
        pingError?.let { throw it }
        connected = true
        return "PONG"
    }

    override suspend fun info(section: String?): String {
        infoError?.let { throw it }
        return if (section == "keyspace") keyspaceRaw else infoRaw
    }

    override suspend fun selectDatabase(index: Int) {
        selectedDatabase = index
    }

    override suspend fun databaseSize(): Long = 12_480

    override suspend fun scan(
        cursor: Long,
        pattern: String?,
        count: Int,
    ): ScanPage = ScanPage(cursor = 0, keys = emptyList())

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

    override suspend fun execute(args: List<String>): RedisReply = RedisReply.Nil

    override fun close() {
        connected = false
    }

    companion object {
        val DEFAULT_KEYSPACE =
            """
            # Keyspace
            db0:keys=12480,expires=30,avg_ttl=1000
            db2:keys=3900,expires=0,avg_ttl=0
            """.trimIndent()

        val DEFAULT_INFO =
            """
            # Server
            redis_version:7.4.2
            uptime_in_seconds:12345

            # Memory
            used_memory:1331439861
            maxmemory:2147483648

            # Clients
            connected_clients:24

            # Stats
            keyspace_hits:100
            keyspace_misses:4
            total_commands_processed:900
            instantaneous_ops_per_second:12.5

            # Replication
            role:master

            # Persistence
            rdb_last_save_time:1700000000
            aof_enabled:0

            # Keyspace
            db0:keys=12480,expires=30,avg_ttl=1000
            db2:keys=3900,expires=0,avg_ttl=0
            """.trimIndent()
    }
}

class RecordingSettingsRepository : SettingsRepository {
    var saved: AppSettings? = null

    override suspend fun load(): AppSettings = saved ?: AppSettings()

    override suspend fun save(settings: AppSettings) {
        saved = settings
    }
}

class RecordingConnectionRepository : ConnectionRepository {
    var saved: List<ConnectionProfile>? = null

    override suspend fun load(): List<ConnectionProfile> = saved.orEmpty()

    override suspend fun save(connections: List<ConnectionProfile>) {
        saved = connections
    }
}

private object IdentityPasswordCipher : PasswordCipher {
    override fun encrypt(plain: String): String = plain

    override fun decrypt(stored: String): String = stored
}

class FakeAppContainer(
    override val initialSettings: AppSettings = AppSettings(),
    override val initialConnections: List<ConnectionProfile> = emptyList(),
    val gateway: FakeRedisGateway = FakeRedisGateway(),
) : AppContainer {
    override val settingsRepository = RecordingSettingsRepository()
    override val connectionRepository = RecordingConnectionRepository()
    override val gatewayFactory: RedisGatewayFactory = RedisGatewayFactory { gateway }
    override val passwordCipher: PasswordCipher = IdentityPasswordCipher
}
