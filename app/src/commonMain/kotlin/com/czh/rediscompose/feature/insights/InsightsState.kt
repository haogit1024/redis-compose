package com.czh.rediscompose.feature.insights

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.czh.rediscompose.app.RedisSession
import com.czh.rediscompose.data.redis.InfoParser
import com.czh.rediscompose.design.component.ToastKind
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.domain.command.ClientConnection
import com.czh.rediscompose.domain.command.CommandStat
import com.czh.rediscompose.domain.command.SlowLogEntry
import com.czh.rediscompose.domain.server.MetricsSample
import com.czh.rediscompose.domain.server.ServerSnapshot
import com.czh.rediscompose.util.currentEpochMillis
import com.czh.rediscompose.util.currentEpochSeconds
import com.czh.rediscompose.util.formatBytes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Sampling state for one insights tab.
 *
 * The page polls INFO every five seconds while it is visible, keeps at most one hour of
 * samples in memory, and refreshes SLOWLOG / CLIENT LIST every third round. Leaving the
 * page calls [stop]; the samples stay so returning restores the chart immediately.
 */
class InsightsState(
    val session: RedisSession,
    private val scope: CoroutineScope,
    private val onToast: (String, ToastKind) -> Unit = { _, _ -> },
) {
    private val insightsJob = SupervisorJob(scope.coroutineContext.job)
    private val insightsScope = CoroutineScope(scope.coroutineContext + insightsJob)

    var snapshot by mutableStateOf<ServerSnapshot?>(null)
        private set

    var commandStats by mutableStateOf<List<CommandStat>>(emptyList())
        private set

    var slowLog by mutableStateOf<List<SlowLogEntry>>(emptyList())
        private set

    var clients by mutableStateOf<List<ClientConnection>>(emptyList())
        private set

    val samples = mutableStateListOf<MetricsSample>()

    var range by mutableStateOf(InsightsRange.LAST_5_MINUTES)
        private set

    var autoRefresh by mutableStateOf(true)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var lastUpdatedEpochMillis by mutableStateOf<Long?>(null)
        private set

    var isPolling by mutableStateOf(false)
        private set

    private var pollJob: Job? = null
    private var refreshJob: Job? = null
    private var pollGeneration = 0

    fun start() {
        if (!insightsScope.isActive || pollJob?.isActive == true) return
        val generation = ++pollGeneration
        isPolling = true
        pollJob =
            insightsScope.launch {
                try {
                    sample(includeExtras = true)
                    var round = 1
                    while (isActive) {
                        delay(InsightsMetrics.SAMPLE_INTERVAL_MILLIS)
                        if (!autoRefresh) continue
                        sample(includeExtras = round % InsightsMetrics.EXTRAS_EVERY_ROUNDS == 0)
                        round += 1
                    }
                } finally {
                    if (generation == pollGeneration) isPolling = false
                }
            }
    }

    fun stop() {
        pollGeneration += 1
        pollJob?.cancel()
        pollJob = null
        isPolling = false
    }

    /** One manual refresh; the poll loop (if any) keeps running independently. */
    fun refreshNow() {
        if (refreshJob?.isActive == true) return
        refreshJob =
            insightsScope.launch {
                isLoading = true
                try {
                    sample(includeExtras = true)
                } finally {
                    isLoading = false
                    refreshJob = null
                }
            }
    }

    fun refresh() = refreshNow()

    fun selectRange(value: InsightsRange) {
        range = value
    }

    fun updateAutoRefresh(enabled: Boolean) {
        autoRefresh = enabled
    }

    fun reportCopied() {
        onToast("已复制慢查询命令", ToastKind.Success)
    }

    fun dispose() {
        stop()
        refreshJob?.cancel()
        refreshJob = null
        insightsScope.cancel()
    }

    /** Snapshot of everything the dashboard renders; pure formatting lives here. */
    fun dashboardData(): InsightsDashboardData {
        val nowMillis = currentEpochMillis()
        val visible = InsightsMetrics.filterSamples(samples, range)
        val latest = samples.lastOrNull()
        val previous = samples.getOrNull(samples.lastIndex - 1)
        val summary = snapshot
        val nowSeconds = currentEpochSeconds()
        val keyspaceSection = summary?.sections?.get("Keyspace").orEmpty()
        return InsightsDashboardData(
            header =
                InsightsHeaderData(
                    host = session.profile.host,
                    port = session.profile.port,
                    version = summary?.version ?: session.summary?.version,
                    uptimeSeconds = summary?.uptimeSeconds ?: session.summary?.uptimeSeconds,
                    range = range,
                    autoRefresh = autoRefresh,
                    isLoading = isLoading,
                    lastUpdatedEpochMillis = lastUpdatedEpochMillis,
                    error = error,
                ),
            kpis = buildKpis(visible, latest, previous),
            throughput = buildThroughput(visible),
            memory = InsightsMetrics.buildMemoryStats(summary),
            commands = InsightsMetrics.commandShares(previous, latest),
            commandsSampling = samples.size < 2,
            keyspace =
                KeyspaceData(
                    available = InsightsMetrics.hasKeyspace(keyspaceSection),
                    rows = InsightsMetrics.keyspaceRows(keyspaceSection),
                ),
            health = InsightsMetrics.healthItems(summary, nowSeconds),
            slowLog = slowLog,
            clients = InsightsMetrics.clientsSummary(clients, summary),
        )
    }

    private suspend fun sample(includeExtras: Boolean) {
        val initialLoad = samples.isEmpty()
        if (initialLoad) isLoading = true
        try {
            val parsed = InfoParser.snapshot(session.gateway.info())
            snapshot = parsed
            val stats =
                try {
                    session.gateway.commandStats()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    error = "命令统计读取失败：${failure.firstLine()}"
                    null
                }
            if (stats != null) commandStats = stats
            appendSample(parsed)
            if (stats != null) error = null
            if (includeExtras) refreshExtras()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            error = "指标刷新失败：${failure.firstLine()}"
        } finally {
            if (initialLoad) isLoading = false
        }
    }

    private fun appendSample(parsed: ServerSnapshot) {
        val now = currentEpochMillis()
        val previous = samples.lastOrNull()
        val totalCommands =
            parsed.totalCommandsProcessed
                ?: previous?.totalCommands
                ?: 0L
        val elapsedSeconds =
            previous?.let { (now - it.atEpochMillis) / 1_000.0 }?.takeIf { it > 0.0 }
        val derivedOps =
            if (previous != null && elapsedSeconds != null) {
                (totalCommands - previous.totalCommands).coerceAtLeast(0L) / elapsedSeconds
            } else {
                0.0
            }
        val ops = parsed.instantaneousOpsPerSecond ?: derivedOps
        val hitRate = InsightsMetrics.hitRateOf(parsed.keyspaceHits, parsed.keyspaceMisses) ?: 0.0
        samples +=
            MetricsSample(
                atEpochMillis = now,
                usedMemoryBytes = parsed.usedMemoryBytes ?: 0L,
                opsPerSecond = ops,
                hitRate = hitRate,
                connectedClients = parsed.connectedClients ?: 0,
                totalCommands = totalCommands,
                commandCalls = commandStats.associate { it.name.lowercase() to it.calls },
            )
        InsightsMetrics.pruneSamples(samples, now)
        lastUpdatedEpochMillis = now
    }

    private suspend fun refreshExtras() {
        try {
            slowLog = session.gateway.slowLog(10)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // SLOWLOG may be disabled or renamed; keep the previous rows.
        }
        try {
            clients = session.gateway.clients()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // CLIENT LIST may be restricted by ACL; keep the previous rows.
        }
    }

    private fun buildKpis(
        visible: List<MetricsSample>,
        latest: MetricsSample?,
        previous: MetricsSample?,
    ): List<KpiData> {
        val summary = snapshot
        val usedMemory = latest?.usedMemoryBytes ?: summary?.usedMemoryBytes
        val maxMemory = summary?.maxMemoryBytes
        val memoryDelta =
            if (visible.size >= 2) {
                latest?.usedMemoryBytes?.minus(visible.first().usedMemoryBytes)
            } else {
                null
            }
        val memorySpark = visible.map { it.usedMemoryBytes.toFloat() }
        val opsAverage = visible.takeIf { it.isNotEmpty() }?.map { it.opsPerSecond }?.average()
        val hitRate = InsightsMetrics.hitRateOf(summary?.keyspaceHits, summary?.keyspaceMisses)
        val hitDelta =
            if (previous != null && hitRate != null && previous.hitRate > 0.0) {
                hitRate - previous.hitRate
            } else {
                null
            }
        val clients = latest?.connectedClients ?: summary?.connectedClients
        val clientDelta = if (previous != null && latest != null) latest.connectedClients - previous.connectedClients else null
        val maxClients = summary?.sections?.get("Clients")?.get("maxclients")?.toIntOrNull()

        return listOf(
            KpiData(
                title = "USED MEMORY",
                icon = AppIcons.Memory,
                value = formatBytes(usedMemory),
                unit = if (maxMemory != null && maxMemory > 0L) "/ ${formatBytes(maxMemory)}" else "无上限",
                delta = memoryDelta?.takeIf { it != 0L }?.let { "${signOf(it)}${formatBytes(abs(it))} · 区间" },
                deltaTone = KpiDeltaTone.Neutral,
                sparkline = memorySpark,
            ),
            KpiData(
                title = "OPS / SEC",
                icon = AppIcons.Gauge,
                value = InsightsMetrics.formatOps(latest?.opsPerSecond),
                unit = "当前",
                delta = opsAverage?.let { "均值 ${InsightsMetrics.formatOps(it)}" },
                deltaTone = KpiDeltaTone.Positive,
                sparkline = visible.map { it.opsPerSecond.toFloat() },
            ),
            KpiData(
                title = "CACHE HIT RATE",
                icon = AppIcons.Percent,
                value = hitRate?.let { InsightsMetrics.formatPercent(it) } ?: "—",
                delta = hitDelta?.let { "${signOf(it * 100.0)}${InsightsMetrics.formatPercent(abs(it))} · 较上次" },
                deltaTone = if (hitDelta != null && hitDelta < 0.0) KpiDeltaTone.Warning else KpiDeltaTone.Positive,
                sparkline = visible.map { (it.hitRate * 100.0).toFloat() },
            ),
            KpiData(
                title = "CONNECTED CLIENTS",
                icon = AppIcons.Clients,
                value = clients?.toString() ?: "—",
                unit = maxClients?.let { "/ $it max" },
                delta = clientDelta?.takeIf { it != 0 }?.let { "${signOf(it.toLong())}${abs(it)} · 较上次" },
                deltaTone = if (clientDelta != null && clientDelta > 0) KpiDeltaTone.Positive else KpiDeltaTone.Neutral,
                sparkline = visible.map { it.connectedClients.toFloat() },
            ),
        )
    }

    private fun buildThroughput(visible: List<MetricsSample>): ThroughputData {
        if (visible.isEmpty()) return ThroughputData()
        val withSeconds = range == InsightsRange.LAST_5_MINUTES || range == InsightsRange.LAST_15_MINUTES
        return ThroughputData(
            labels = InsightsMetrics.axisSamples(visible).map { InsightsMetrics.axisLabel(it, withSeconds) },
            total = visible.map { it.opsPerSecond.toFloat() },
            reads =
                visible.mapIndexed { index, sample ->
                    InsightsMetrics.readOpsPerSecond(visible.getOrNull(index - 1), sample).toFloat()
                },
        )
    }

    private fun signOf(value: Long): String = if (value > 0) "+" else "−"

    private fun signOf(value: Double): String = if (value >= 0.0) "+" else "−"

    private fun Throwable.firstLine(): String =
        message?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() }
            ?: this::class.simpleName
            ?: "未知错误"
}
