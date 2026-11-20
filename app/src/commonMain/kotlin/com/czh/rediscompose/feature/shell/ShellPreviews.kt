package com.czh.rediscompose.feature.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.czh.rediscompose.app.AppContainer
import com.czh.rediscompose.app.AppState
import com.czh.rediscompose.data.redis.HashFieldBytes
import com.czh.rediscompose.data.redis.RedisGateway
import com.czh.rediscompose.data.redis.RedisGatewayFactory
import com.czh.rediscompose.data.redis.SortedSetEntryBytes
import com.czh.rediscompose.data.redis.StreamEntryBytes
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.domain.command.ClientConnection
import com.czh.rediscompose.domain.command.CommandStat
import com.czh.rediscompose.domain.command.RedisReply
import com.czh.rediscompose.domain.command.SlowLogEntry
import com.czh.rediscompose.domain.connection.ConnectionColor
import com.czh.rediscompose.domain.connection.ConnectionEnvironment
import com.czh.rediscompose.domain.connection.ConnectionProfile
import com.czh.rediscompose.domain.connection.ConnectionRepository
import com.czh.rediscompose.domain.key.KeyType
import com.czh.rediscompose.domain.key.ScanPage
import com.czh.rediscompose.domain.repository.PasswordCipher
import com.czh.rediscompose.domain.settings.AppSettings
import com.czh.rediscompose.domain.settings.SettingsRepository
import com.czh.rediscompose.domain.settings.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * Preview-only container and gateway.
 *
 * They never touch the user's files or a real Redis instance; [previewStateWithConnections]
 * uses a connected fake session so the shell can be inspected visually without opening a
 * network connection.
 */
private class PreviewSettingsRepository : SettingsRepository {
    override suspend fun load(): AppSettings = AppSettings()

    override suspend fun save(settings: AppSettings) = Unit
}

private class PreviewConnectionRepository : ConnectionRepository {
    override suspend fun load(): List<ConnectionProfile> = emptyList()

    override suspend fun save(connections: List<ConnectionProfile>) = Unit
}

private object PreviewPasswordCipher : PasswordCipher {
    override fun encrypt(plain: String): String = plain

    override fun decrypt(stored: String): String = stored
}

private class PreviewContainer(
    initialSettings: AppSettings,
    initialConnections: List<ConnectionProfile>,
) : AppContainer {
    override val settingsRepository: SettingsRepository = PreviewSettingsRepository()
    override val connectionRepository: ConnectionRepository = PreviewConnectionRepository()
    override val gatewayFactory: RedisGatewayFactory = RedisGatewayFactory { PreviewGateway() }
    override val passwordCipher: PasswordCipher = PreviewPasswordCipher
    override val initialSettings: AppSettings = initialSettings
    override val initialConnections: List<ConnectionProfile> = initialConnections
}

private class PreviewGateway : RedisGateway {
    override suspend fun ping(): String = "PONG"

    override suspend fun info(section: String?): String = if (section == "keyspace") PREVIEW_KEYSPACE else PREVIEW_INFO

    override suspend fun databaseCount(): Int? = 16

    override suspend fun selectDatabase(index: Int) = Unit

    override suspend fun databaseSize(): Long = 12_480

    override suspend fun scan(
        cursor: Long,
        pattern: String?,
        count: Int,
    ): ScanPage = ScanPage(cursor = 0, keys = emptyList())

    override suspend fun keyType(key: String): KeyType = KeyType.STRING

    override suspend fun keyTtl(key: String): Long = 42

    override suspend fun keyMemoryUsage(key: String): Long? = 1_024

    override suspend fun keyEncoding(key: String): String? = "embstr"

    override suspend fun getString(key: String): ByteArray = "preview".encodeToByteArray()

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

    override suspend fun getStreamGroupCount(key: String): Int? = 0

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

private val PREVIEW_KEYSPACE =
    """
    # Keyspace
    db0:keys=12480,expires=30,avg_ttl=1000
    db1:keys=8200,expires=0,avg_ttl=0
    db2:keys=3900,expires=0,avg_ttl=0
    """.trimIndent()

private val PREVIEW_INFO =
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
    db0:keys=12480,expires=30,avg_ttl=1000
    db1:keys=8200,expires=0,avg_ttl=0
    db2:keys=3900,expires=0,avg_ttl=0
    """.trimIndent()

private val PREVIEW_CONNECTIONS =
    listOf(
        ConnectionProfile(
            id = "prod-cache",
            name = "Prod Cache",
            environment = ConnectionEnvironment.PRODUCTION,
            host = "redis-01.acme.io",
            port = 6379,
            color = ConnectionColor.IRIS,
            group = "Production",
            lastOpenedAtEpochMillis = 1_700_000_300_000,
        ),
        ConnectionProfile(
            id = "session-store",
            name = "Session Store",
            environment = ConnectionEnvironment.PRODUCTION,
            host = "127.0.0.1",
            port = 6380,
            color = ConnectionColor.MINT,
            group = "Production",
            lastOpenedAtEpochMillis = 1_700_000_200_000,
        ),
        ConnectionProfile(
            id = "billing-staging",
            name = "Billing Staging",
            environment = ConnectionEnvironment.STAGING,
            host = "staging-billing.acme.io",
            port = 6379,
            color = ConnectionColor.AMBER,
            group = "Staging",
            lastOpenedAtEpochMillis = 1_700_000_100_000,
        ),
        ConnectionProfile(
            id = "local-sandbox",
            name = "Local Sandbox",
            environment = ConnectionEnvironment.LOCAL,
            host = "127.0.0.1",
            port = 6379,
            color = ConnectionColor.SKY,
            group = "Local",
            lastOpenedAtEpochMillis = 1_700_000_000_000,
        ),
    )

private fun previewScope(): CoroutineScope = CoroutineScope(Dispatchers.Unconfined)

internal fun previewEmptyState(): AppState =
    AppState(
        container = PreviewContainer(AppSettings(theme = ThemeMode.DARK), emptyList()),
        scope = previewScope(),
        initialSettings = AppSettings(theme = ThemeMode.DARK),
        initialConnections = emptyList(),
    )

internal fun previewStateWithConnections(openFirst: Boolean = false): AppState {
    val container = PreviewContainer(AppSettings(theme = ThemeMode.DARK), PREVIEW_CONNECTIONS)
    val state =
        AppState(
            container = container,
            scope = previewScope(),
            initialSettings = AppSettings(theme = ThemeMode.DARK),
            initialConnections = PREVIEW_CONNECTIONS,
        )
    if (openFirst) state.openConnection(PREVIEW_CONNECTIONS.first().id)
    return state
}

@Preview(widthDp = 1440, heightDp = 900)
@Composable
private fun AppShellDarkPreview() {
    val state = remember { previewStateWithConnections(openFirst = true) }
    AppTheme(dark = true) {
        AppShell(state = state, dark = true)
    }
}

@Preview(widthDp = 1120, heightDp = 720)
@Composable
private fun AppShellLightPreview() {
    val state = remember { previewStateWithConnections(openFirst = true) }
    AppTheme(dark = false) {
        AppShell(state = state, dark = false)
    }
}

@Preview(widthDp = 1440, heightDp = 900)
@Composable
private fun AppShellEmptyPreview() {
    val state = remember { previewEmptyState() }
    AppTheme(dark = true) {
        AppShell(state = state, dark = true)
    }
}
