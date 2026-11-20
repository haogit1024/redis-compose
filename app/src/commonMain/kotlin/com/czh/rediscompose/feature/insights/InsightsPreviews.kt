package com.czh.rediscompose.feature.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.app.RedisSession
import com.czh.rediscompose.data.redis.HashFieldBytes
import com.czh.rediscompose.data.redis.InfoParser
import com.czh.rediscompose.data.redis.RedisGateway
import com.czh.rediscompose.data.redis.SortedSetEntryBytes
import com.czh.rediscompose.data.redis.StreamEntryBytes
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.domain.command.ClientConnection
import com.czh.rediscompose.domain.command.CommandStat
import com.czh.rediscompose.domain.command.RedisReply
import com.czh.rediscompose.domain.command.SlowLogEntry
import com.czh.rediscompose.domain.connection.ConnectionProfile
import com.czh.rediscompose.domain.key.KeyType
import com.czh.rediscompose.domain.key.ScanPage
import com.czh.rediscompose.domain.server.MetricsSample
import com.czh.rediscompose.domain.server.ServerSnapshot
import com.czh.rediscompose.feature.insights.chart.AreaChart
import com.czh.rediscompose.feature.insights.chart.ChartSeriesData
import com.czh.rediscompose.feature.insights.chart.DonutChart
import com.czh.rediscompose.feature.insights.chart.Sparkline
import com.czh.rediscompose.util.currentEpochMillis
import com.czh.rediscompose.util.currentEpochSeconds
import com.czh.rediscompose.util.formatBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Preview-only fixtures.
 *
 * The gateway is fully in-memory; previews never read `~/.redis-compose` or open a socket.
 */
private val PREVIEW_PROFILE =
    ConnectionProfile(
        id = "preview",
        name = "Preview",
        host = "redis-01.acme.io",
        port = 6379,
    )

private fun previewInfo(): String {
    val now = currentEpochSeconds()
    val raw =
        """
        # Server
        redis_version:7.4.2
        uptime_in_seconds:3651600

        # Memory
        used_memory:1331439861
        used_memory_peak:1588000000
        maxmemory:2147483648
        maxmemory_policy:allkeys-lru
        mem_fragmentation_ratio:1.11

        # Clients
        connected_clients:24
        maxclients:256

        # Stats
        keyspace_hits:98400000
        keyspace_misses:1290000
        total_commands_processed:91200000
        instantaneous_ops_per_second:4820
        evicted_keys:1204

        # Replication
        role:master
        connected_slaves:0

        # Persistence
        rdb_last_save_time:${now - 2400}
        rdb_last_bgsave_status:ok
        aof_enabled:1
        aof_current_size:12582912
        aof_last_bgrewrite_status:ok

        # Keyspace
        db0:keys=12480,expires=3218,avg_ttl=16920000
        db1:keys=8201,expires=1840,avg_ttl=11520000
        db2:keys=3904,expires=602,avg_ttl=5400000
        db3:keys=0,expires=0,avg_ttl=0
        db4:keys=0,expires=0,avg_ttl=0
        """.trimIndent()
    return raw
}

private fun previewSnapshot(): ServerSnapshot = InfoParser.snapshot(previewInfo())

private fun previewSamples(count: Int = 60): List<MetricsSample> {
    val end = 1_700_000_000_000L
    return (0 until count).map { index ->
        val wave = sin(index / 4.0)
        val slowWave = cos(index / 9.0)
        MetricsSample(
            atEpochMillis = end - (count - 1 - index) * 5_000L,
            usedMemoryBytes = 1_280_000_000L + index * 1_000_000L + (wave * 4_000_000).toLong(),
            opsPerSecond = 4_600.0 + wave * 700.0 + slowWave * 300.0,
            hitRate = 0.982 + sin(index / 10.0) * 0.004,
            connectedClients = 22 + index % 3,
            totalCommands = 9_000_000L + index * 24_000L,
            commandCalls =
                mapOf(
                    "get" to 100_000L + index * 4_000L,
                    "hgetall" to 80_000L + index * 2_600L,
                    "set" to 50_000L + index * 1_100L,
                    "scan" to 30_000L + index * 800L,
                    "zrange" to 20_000L + index * 600L,
                    "expire" to 15_000L + index * 300L,
                    "del" to 5_000L + index * 80L,
                ),
        )
    }
}

private fun previewSlowLog(): List<SlowLogEntry> =
    listOf(
        SlowLogEntry(1, 1_700_000_000, 18_400, listOf("GET", "cache:product:8842"), "10.0.4.18", "worker-3"),
        SlowLogEntry(2, 1_699_999_900, 12_000, listOf("HGETALL", "cache:user:1001:profile"), "10.0.4.6", "worker-1"),
        SlowLogEntry(3, 1_699_999_800, 10_800, listOf("ZRANGE", "queue:jobs:priority", "0", "-1"), "10.0.4.22", "worker-7"),
        SlowLogEntry(4, 1_699_999_700, 10_200, listOf("SCAN", "0", "MATCH", "*", "COUNT", "1000"), "10.0.4.9", "redis-compose"),
    )

private fun previewClients(): List<ClientConnection> {
    val workers =
        (1..18).map { index ->
            ClientConnection("$index", "10.0.4.$index", "worker-$index", 0, index % 40L)
        }
    val management = ClientConnection("app", "10.0.4.100", "redis-compose", 0, 1)
    val others =
        (1..5).map { index ->
            ClientConnection("o$index", "10.0.5.$index", "", 0, 80L + index)
        }
    return workers + management + others
}

private fun previewKpis(
    samples: List<MetricsSample>,
    snapshot: ServerSnapshot,
): List<KpiData> {
    val latest = samples.lastOrNull()
    val first = samples.firstOrNull()
    val used = latest?.usedMemoryBytes ?: snapshot.usedMemoryBytes
    val max = snapshot.maxMemoryBytes
    val memoryDelta = if (latest != null && first != null) latest.usedMemoryBytes - first.usedMemoryBytes else null
    val average = samples.takeIf { it.isNotEmpty() }?.map { it.opsPerSecond }?.average()
    val hitRate = InsightsMetrics.hitRateOf(snapshot.keyspaceHits, snapshot.keyspaceMisses)
    val clients = latest?.connectedClients ?: snapshot.connectedClients
    return listOf(
        KpiData(
            title = "USED MEMORY",
            icon = AppIcons.Memory,
            value = formatBytes(used),
            unit = if (max != null && max > 0L) "/ ${formatBytes(max)}" else "无上限",
            delta = memoryDelta?.let { "+${formatBytes(it)} · 区间" },
            sparkline = samples.map { it.usedMemoryBytes.toFloat() },
        ),
        KpiData(
            title = "OPS / SEC",
            icon = AppIcons.Gauge,
            value = InsightsMetrics.formatOps(latest?.opsPerSecond),
            unit = "当前",
            delta = average?.let { "均值 ${InsightsMetrics.formatOps(it)}" },
            deltaTone = KpiDeltaTone.Positive,
            sparkline = samples.map { it.opsPerSecond.toFloat() },
        ),
        KpiData(
            title = "CACHE HIT RATE",
            icon = AppIcons.Percent,
            value = hitRate?.let { InsightsMetrics.formatPercent(it) } ?: "—",
            delta = "较上次 +0.1%",
            deltaTone = KpiDeltaTone.Positive,
            sparkline = samples.map { (it.hitRate * 100.0).toFloat() },
        ),
        KpiData(
            title = "CONNECTED CLIENTS",
            icon = AppIcons.Clients,
            value = clients?.toString() ?: "—",
            unit = "/ ${snapshot.sections["Clients"]?.get("maxclients") ?: "—"} max",
            delta = "+1 · 较上次",
            sparkline = samples.map { it.connectedClients.toFloat() },
        ),
    )
}

private fun previewThroughput(samples: List<MetricsSample>): ThroughputData {
    if (samples.isEmpty()) return ThroughputData()
    return ThroughputData(
        labels = InsightsMetrics.axisSamples(samples).map { InsightsMetrics.axisLabel(it, withSeconds = true) },
        total = samples.map { it.opsPerSecond.toFloat() },
        reads =
            samples.mapIndexed { index, sample ->
                InsightsMetrics.readOpsPerSecond(samples.getOrNull(index - 1), sample).toFloat()
            },
    )
}

private fun previewDashboard(
    withSamples: Boolean = true,
    withMaxMemory: Boolean = true,
    error: String? = null,
): InsightsDashboardData {
    val snapshot =
        previewSnapshot().let { parsed ->
            if (withMaxMemory) parsed else parsed.copy(maxMemoryBytes = 0L)
        }
    val samples = if (withSamples) previewSamples() else emptyList()
    val latest = samples.lastOrNull()
    val previous = samples.getOrNull(samples.lastIndex - 1)
    val memory = InsightsMetrics.buildMemoryStats(snapshot)
    return InsightsDashboardData(
        header =
            InsightsHeaderData(
                host = PREVIEW_PROFILE.host,
                port = PREVIEW_PROFILE.port,
                version = snapshot.version,
                uptimeSeconds = snapshot.uptimeSeconds,
                range = InsightsRange.LAST_5_MINUTES,
                autoRefresh = true,
                lastUpdatedEpochMillis = if (withSamples) currentEpochMillis() - 3_000L else null,
                error = error,
            ),
        kpis = previewKpis(samples, snapshot),
        throughput = previewThroughput(samples),
        memory = memory,
        commands = InsightsMetrics.commandShares(previous, latest),
        commandsSampling = samples.size < 2,
        keyspace =
            KeyspaceData(
                available = true,
                rows = InsightsMetrics.keyspaceRows(snapshot.sections["Keyspace"].orEmpty()),
            ),
        health = InsightsMetrics.healthItems(snapshot, currentEpochSeconds()),
        slowLog = previewSlowLog(),
        clients = InsightsMetrics.clientsSummary(previewClients(), snapshot),
    )
}

private class PreviewInsightsGateway : RedisGateway {
    private val info: String = previewInfo()
    private val stats: List<CommandStat> =
        listOf(
            CommandStat("get", 1_120_000, 12_000_000, 10.7),
            CommandStat("hgetall", 870_000, 18_000_000, 20.7),
            CommandStat("set", 510_000, 6_000_000, 11.8),
            CommandStat("scan", 320_000, 9_000_000, 28.1),
            CommandStat("zrange", 210_000, 3_000_000, 14.3),
            CommandStat("expire", 120_000, 900_000, 7.5),
        )

    override suspend fun ping(): String = "PONG"

    override suspend fun info(section: String?): String = info

    override suspend fun databaseCount(): Int? = 16

    override suspend fun selectDatabase(index: Int) = Unit

    override suspend fun databaseSize(): Long = 24_585L

    override suspend fun scan(
        cursor: Long,
        pattern: String?,
        count: Int,
    ): ScanPage = ScanPage(cursor = 0, keys = emptyList())

    override suspend fun keyType(key: String): KeyType = KeyType.STRING

    override suspend fun keyTtl(key: String): Long = -1L

    override suspend fun keyMemoryUsage(key: String): Long? = 1_024L

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

    override suspend fun slowLog(count: Int): List<SlowLogEntry> = previewSlowLog()

    override suspend fun clients(): List<ClientConnection> = previewClients()

    override suspend fun commandStats(): List<CommandStat> = stats

    override suspend fun execute(args: List<String>): RedisReply = RedisReply.Nil

    override fun close() = Unit
}

private fun previewInsightsState(): InsightsState {
    val scope = CoroutineScope(Dispatchers.Unconfined)
    val session = RedisSession(profile = PREVIEW_PROFILE, gateway = PreviewInsightsGateway(), appScope = scope)
    scope.launch { session.connect() }
    val state = InsightsState(session = session, scope = scope)
    state.refreshNow()
    state.samples.clear()
    state.samples.addAll(previewSamples())
    state.selectRange(InsightsRange.SESSION)
    return state
}

@Preview(widthDp = 1440, heightDp = 900)
@Composable
private fun InsightsScreenDarkPreview() {
    val state = remember { previewInsightsState() }
    AppTheme(dark = true) {
        InsightsScreen(state = state)
    }
}

@Preview(widthDp = 1120, heightDp = 720)
@Composable
private fun InsightsScreenLightPreview() {
    val state = remember { previewInsightsState() }
    AppTheme(dark = false) {
        InsightsScreen(state = state)
    }
}

@Preview(widthDp = 1440, heightDp = 900)
@Composable
private fun InsightsDashboardEmptyPreview() {
    AppTheme(dark = true) {
        InsightsDashboard(
            data = previewDashboard(withSamples = false, error = "指标刷新失败：连接已断开"),
            onRangeChange = {},
            onAutoRefreshChange = {},
            onRefresh = {},
            onCopyCommand = {},
            onOpenConsole = {},
        )
    }
}

@Preview(widthDp = 1120, heightDp = 720)
@Composable
private fun InsightsDashboardNoMaxMemoryPreview() {
    AppTheme(dark = false) {
        InsightsDashboard(
            data = previewDashboard(withMaxMemory = false),
            onRangeChange = {},
            onAutoRefreshChange = {},
            onRefresh = {},
            onCopyCommand = {},
            onOpenConsole = {},
        )
    }
}

@Preview(widthDp = 1440, heightDp = 900)
@Composable
private fun InsightsHeaderPreview() {
    AppTheme(dark = true) {
        Column(Modifier.background(LocalAppColors.current.background).padding(22.dp)) {
            InsightsHeader(
                data = previewDashboard().header,
                onRangeChange = {},
                onAutoRefreshChange = {},
                onRefresh = {},
            )
        }
    }
}

@Preview(widthDp = 1100, heightDp = 150)
@Composable
private fun KpiGridPreview() {
    AppTheme(dark = true) {
        KpiGrid(
            kpis = previewDashboard().kpis,
            modifier = Modifier.background(LocalAppColors.current.background).padding(16.dp),
        )
    }
}

@Preview(widthDp = 760, heightDp = 320)
@Composable
private fun ThroughputCardPreview() {
    AppTheme(dark = false) {
        ThroughputCard(
            data = previewDashboard().throughput,
            modifier = Modifier.background(LocalAppColors.current.background).padding(16.dp),
        )
    }
}

@Preview(widthDp = 380, heightDp = 330)
@Composable
private fun MemoryCardPreview() {
    AppTheme(dark = true) {
        MemoryCard(
            stats = previewDashboard().memory,
            modifier = Modifier.background(LocalAppColors.current.background).padding(16.dp),
        )
    }
}

@Preview(widthDp = 380, heightDp = 300)
@Composable
private fun MemoryCardNoLimitPreview() {
    AppTheme(dark = false) {
        MemoryCard(
            stats = previewDashboard(withMaxMemory = false).memory,
            modifier = Modifier.background(LocalAppColors.current.background).padding(16.dp),
        )
    }
}

@Preview(widthDp = 380, heightDp = 330)
@Composable
private fun CommandDistributionCardPreview() {
    AppTheme(dark = true) {
        val data = previewDashboard()
        CommandDistributionCard(
            shares = data.commands,
            sampling = data.commandsSampling,
            modifier = Modifier.background(LocalAppColors.current.background).padding(16.dp),
        )
    }
}

@Preview(widthDp = 380, heightDp = 420)
@Composable
private fun KeyspaceCardPreview() {
    AppTheme(dark = false) {
        KeyspaceCard(
            keyspace = previewDashboard().keyspace,
            modifier = Modifier.background(LocalAppColors.current.background).padding(16.dp),
        )
    }
}

@Preview(widthDp = 380, heightDp = 260)
@Composable
private fun HealthCardPreview() {
    AppTheme(dark = true) {
        HealthCard(
            items = previewDashboard().health,
            modifier = Modifier.background(LocalAppColors.current.background).padding(16.dp),
        )
    }
}

@Preview(widthDp = 560, heightDp = 300)
@Composable
private fun SlowLogCardPreview() {
    AppTheme(dark = true) {
        SlowLogCard(
            entries = previewSlowLog(),
            modifier = Modifier.background(LocalAppColors.current.background).padding(16.dp),
            onOpenConsole = {},
        )
    }
}

@Preview(widthDp = 380, heightDp = 420)
@Composable
private fun ClientsCardPreview() {
    AppTheme(dark = false) {
        ClientsCard(
            summary = previewDashboard().clients,
            modifier = Modifier.background(LocalAppColors.current.background).padding(16.dp),
        )
    }
}

@Preview(widthDp = 240, heightDp = 90)
@Composable
private fun SparklinePreview() {
    AppTheme(dark = true) {
        Box(
            modifier =
                Modifier
                    .background(LocalAppColors.current.background)
                    .padding(16.dp)
                    .size(width = 180.dp, height = 48.dp),
        ) {
            Sparkline(values = previewSamples().map { it.opsPerSecond.toFloat() })
        }
    }
}

@Preview(widthDp = 720, heightDp = 300)
@Composable
private fun AreaChartPreview() {
    AppTheme(dark = false) {
        val throughput = previewDashboard().throughput
        Box(
            Modifier
                .background(LocalAppColors.current.background)
                .padding(16.dp),
        ) {
            AreaChart(
                labels = throughput.labels,
                series =
                    listOf(
                        ChartSeriesData("命令", throughput.total, LocalAppColors.current.accent, filled = true),
                        ChartSeriesData("读取", throughput.reads, LocalAppColors.current.success),
                    ),
            )
        }
    }
}

@Preview(widthDp = 220, heightDp = 220)
@Composable
private fun DonutChartPreview() {
    AppTheme(dark = true) {
        Box(
            modifier =
                Modifier
                    .background(LocalAppColors.current.background)
                    .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            DonutChart(progress = 0.62f) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("62%", style = LocalAppTypography.current.keyTitle, color = LocalAppColors.current.text)
                    Text("已使用", style = LocalAppTypography.current.monoMicro, color = LocalAppColors.current.textQuaternary)
                }
            }
        }
    }
}
