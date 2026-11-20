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

data class RecordedScan(
    val cursor: Long,
    val pattern: String?,
    val count: Int,
)

/**
 * In-memory backend for AppState and feature tests.
 *
 * Read-only fields (`scanKeys`, `keyTypeByKey`, …) keep the Phase 4 tests working. Writes
 * also update a small in-memory value store, which Phase 5 uses to exercise real save,
 * rename, TTL and delete flows without a Redis server.
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
    var databaseCountValue: Int? = 16

    var databaseSizeValue: Long = 12_480
    var scanKeys: List<String> = emptyList()
    var scanError: Exception? = null
    var keyTypeByKey: Map<String, KeyType> = emptyMap()
    var keyTtlByKey: Map<String, Long> = emptyMap()
    var memoryByKey: Map<String, Long> = emptyMap()
    var encodingByKey: Map<String, String> = emptyMap()
    var writeError: Exception? = null
    var executeReply: RedisReply? = null
    var executeError: Exception? = null

    var slowLogEntries: List<SlowLogEntry> = emptyList()
    var clientConnections: List<ClientConnection> = emptyList()
    var commandStatValues: List<CommandStat> = emptyList()
    var slowLogError: Exception? = null
    var clientsError: Exception? = null
    var commandStatsError: Exception? = null

    val executedCommands = mutableListOf<List<String>>()

    /** Page size used by [scan]; smaller than the requested COUNT keeps tests multi-page. */
    var scanPageSize: Int = 1_000

    val stringValues = mutableMapOf<String, ByteArray>()
    val hashValues = mutableMapOf<String, LinkedHashMap<String, ByteArray>>()
    val listValues = mutableMapOf<String, MutableList<ByteArray>>()
    val setValues = mutableMapOf<String, LinkedHashSet<String>>()
    val zsetValues = mutableMapOf<String, LinkedHashMap<String, Double>>()
    val streamValues = mutableMapOf<String, MutableList<StreamEntryBytes>>()
    val streamGroupCounts = mutableMapOf<String, Int>()
    val ttlValues = mutableMapOf<String, Long>()
    val storeKeys = linkedSetOf<String>()

    val scanCalls = mutableListOf<RecordedScan>()
    val typeRequests = mutableListOf<String>()
    val ttlRequests = mutableListOf<String>()

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

    override suspend fun databaseCount(): Int? = databaseCountValue

    override suspend fun selectDatabase(index: Int) {
        selectedDatabase = index
    }

    override suspend fun databaseSize(): Long = databaseSizeValue

    override suspend fun scan(
        cursor: Long,
        pattern: String?,
        count: Int,
    ): ScanPage {
        scanCalls += RecordedScan(cursor = cursor, pattern = pattern, count = count)
        scanError?.let { throw it }
        val allKeys = (scanKeys + storeKeys).distinct()
        val matched =
            if (pattern.isNullOrBlank()) {
                allKeys
            } else {
                allKeys.filter { matchesGlob(it, pattern) }
            }
        val start = cursor.toInt().coerceIn(0, matched.size)
        val pageSize = minOf(count.coerceAtLeast(1), scanPageSize.coerceAtLeast(1))
        val end = (start + pageSize).coerceAtMost(matched.size)
        val nextCursor = if (end >= matched.size) 0L else end.toLong()
        return ScanPage(cursor = nextCursor, keys = matched.subList(start, end))
    }

    override suspend fun keyType(key: String): KeyType {
        typeRequests += key
        keyTypeByKey[key]?.let { return it }
        return when {
            stringValues.containsKey(key) -> KeyType.STRING
            hashValues.containsKey(key) -> KeyType.HASH
            listValues.containsKey(key) -> KeyType.LIST
            setValues.containsKey(key) -> KeyType.SET
            zsetValues.containsKey(key) -> KeyType.ZSET
            streamValues.containsKey(key) -> KeyType.STREAM
            key in storeKeys || key in scanKeys -> KeyType.STRING
            else -> KeyType.NONE
        }
    }

    override suspend fun keyTtl(key: String): Long {
        ttlRequests += key
        return ttlValues[key] ?: keyTtlByKey[key] ?: -1L
    }

    override suspend fun keyMemoryUsage(key: String): Long? = memoryByKey[key]

    override suspend fun keyEncoding(key: String): String? = encodingByKey[key]

    override suspend fun getString(key: String): ByteArray? = stringValues[key]

    override suspend fun setString(
        key: String,
        value: ByteArray,
    ) {
        checkWrite()
        stringValues[key] = value
        touch(key)
    }

    override suspend fun getHash(key: String): List<HashFieldBytes> =
        hashValues[key]
            .orEmpty()
            .map { (field, value) -> HashFieldBytes(field = field, value = value) }
            .sortedBy { it.field.lowercase() }

    override suspend fun hashSet(
        key: String,
        field: String,
        value: ByteArray,
    ) {
        checkWrite()
        hashValues.getOrPut(key) { linkedMapOf() }[field] = value
        touch(key)
    }

    override suspend fun hashDelete(
        key: String,
        field: String,
    ) {
        checkWrite()
        val hash = hashValues[key] ?: return
        hash.remove(field)
        if (hash.isEmpty()) {
            hashValues.remove(key)
            removeKeyIfEmpty(key)
        }
    }

    override suspend fun getList(key: String): List<ByteArray> = listValues[key].orEmpty()

    override suspend fun listPush(
        key: String,
        value: ByteArray,
        head: Boolean,
    ) {
        checkWrite()
        val list = listValues.getOrPut(key) { mutableListOf() }
        if (head) list.add(0, value) else list.add(value)
        touch(key)
    }

    override suspend fun listSet(
        key: String,
        index: Long,
        value: ByteArray,
    ) {
        checkWrite()
        val list = listValues[key] ?: return
        if (index >= 0 && index < list.size) {
            list[index.toInt()] = value
        }
    }

    override suspend fun listRemove(
        key: String,
        value: ByteArray,
    ) {
        checkWrite()
        val list = listValues[key] ?: return
        val index = list.indexOfFirst { it.contentEquals(value) }
        if (index >= 0) list.removeAt(index)
        if (index >= 0 && list.isEmpty()) {
            listValues.remove(key)
            removeKeyIfEmpty(key)
        }
    }

    override suspend fun getSet(key: String): List<ByteArray> = setValues[key].orEmpty().sorted().map { it.encodeToByteArray() }

    override suspend fun setAdd(
        key: String,
        value: ByteArray,
    ) {
        checkWrite()
        setValues.getOrPut(key) { linkedSetOf() }.add(value.decodeToString())
        touch(key)
    }

    override suspend fun setRemove(
        key: String,
        value: ByteArray,
    ) {
        checkWrite()
        val set = setValues[key] ?: return
        set.remove(value.decodeToString())
        if (set.isEmpty()) {
            setValues.remove(key)
            removeKeyIfEmpty(key)
        }
    }

    override suspend fun getSortedSet(key: String): List<SortedSetEntryBytes> =
        zsetValues[key]
            .orEmpty()
            .map { (member, score) -> SortedSetEntryBytes(member = member.encodeToByteArray(), score = score) }

    override suspend fun sortedSetAdd(
        key: String,
        member: ByteArray,
        score: Double,
    ) {
        checkWrite()
        zsetValues.getOrPut(key) { linkedMapOf() }[member.decodeToString()] = score
        touch(key)
    }

    override suspend fun sortedSetRemove(
        key: String,
        member: ByteArray,
    ) {
        checkWrite()
        val zset = zsetValues[key] ?: return
        zset.remove(member.decodeToString())
        if (zset.isEmpty()) {
            zsetValues.remove(key)
            removeKeyIfEmpty(key)
        }
    }

    override suspend fun getStream(
        key: String,
        count: Int,
    ): List<StreamEntryBytes> = streamValues[key].orEmpty().take(if (count > 0) count else Int.MAX_VALUE)

    override suspend fun getStreamGroupCount(key: String): Int? = streamGroupCounts[key]

    override suspend fun deleteKeys(keys: List<String>) {
        checkWrite()
        keys.forEach { key ->
            stringValues.remove(key)
            hashValues.remove(key)
            listValues.remove(key)
            setValues.remove(key)
            zsetValues.remove(key)
            streamValues.remove(key)
            streamGroupCounts.remove(key)
            ttlValues.remove(key)
            storeKeys.remove(key)
        }
    }

    override suspend fun renameKey(
        oldKey: String,
        newKey: String,
    ) {
        checkWrite()
        stringValues.remove(oldKey)?.let { stringValues[newKey] = it }
        hashValues.remove(oldKey)?.let { hashValues[newKey] = it }
        listValues.remove(oldKey)?.let { listValues[newKey] = it }
        setValues.remove(oldKey)?.let { setValues[newKey] = it }
        zsetValues.remove(oldKey)?.let { zsetValues[newKey] = it }
        streamValues.remove(oldKey)?.let { streamValues[newKey] = it }
        streamGroupCounts.remove(oldKey)?.let { streamGroupCounts[newKey] = it }
        ttlValues.remove(oldKey)?.let { ttlValues[newKey] = it }
        storeKeys.remove(oldKey)
        storeKeys.add(newKey)
    }

    override suspend fun expire(
        key: String,
        seconds: Long,
    ) {
        checkWrite()
        ttlValues[key] = seconds
    }

    override suspend fun persist(key: String) {
        checkWrite()
        ttlValues.remove(key)
        keyTtlByKey = keyTtlByKey - key
    }

    override suspend fun slowLog(count: Int): List<SlowLogEntry> {
        slowLogError?.let { throw it }
        return slowLogEntries.take(count)
    }

    override suspend fun clients(): List<ClientConnection> {
        clientsError?.let { throw it }
        return clientConnections
    }

    override suspend fun commandStats(): List<CommandStat> {
        commandStatsError?.let { throw it }
        return commandStatValues
    }

    override suspend fun execute(args: List<String>): RedisReply {
        executedCommands += args
        executeError?.let { throw it }
        executeReply?.let { return it }
        return when {
            args.size == 2 && args[0].equals("XLEN", ignoreCase = true) ->
                RedisReply.Integer((streamValues[args[1]]?.size ?: 0).toLong())
            else -> RedisReply.Nil
        }
    }

    override fun close() {
        connected = false
    }

    private fun checkWrite() {
        writeError?.let { throw it }
    }

    private fun touch(key: String) {
        storeKeys.add(key)
    }

    private fun removeKeyIfEmpty(key: String) {
        val empty =
            !stringValues.containsKey(key) &&
                !hashValues.containsKey(key) &&
                !listValues.containsKey(key) &&
                !setValues.containsKey(key) &&
                !zsetValues.containsKey(key) &&
                !streamValues.containsKey(key)
        if (empty) {
            storeKeys.remove(key)
            ttlValues.remove(key)
        }
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

/** Minimal Redis glob matcher (`*`, `?`) for SCAN MATCH assertions in tests. */
internal fun matchesGlob(
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
                    '.', '(', ')', '[', ']', '{', '}', '+', '^', '$', '|', '\\' ->
                        append('\\').append(character)
                    else -> append(character)
                }
            }
            append('$')
        }
    return Regex(regex).matches(key)
}
