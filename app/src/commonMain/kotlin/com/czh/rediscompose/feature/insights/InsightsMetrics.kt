package com.czh.rediscompose.feature.insights

import com.czh.rediscompose.domain.command.ClientConnection
import com.czh.rediscompose.domain.server.MetricsSample
import com.czh.rediscompose.domain.server.ServerSnapshot
import com.czh.rediscompose.util.formatBytes
import com.czh.rediscompose.util.formatCount
import com.czh.rediscompose.util.formatEpochSeconds
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Pure derivations for the insights page.
 *
 * Redis offers counters, not history, so every value here is either a snapshot field or a
 * difference between two samples from this session.
 */
object InsightsMetrics {
    const val MAX_SAMPLES: Int = 720
    const val SAMPLE_INTERVAL_MILLIS: Long = 5_000L
    const val SAMPLE_RETENTION_MILLIS: Long = 3_600_000L
    const val EXTRAS_EVERY_ROUNDS: Int = 3

    /** Redis' default `databases` configuration, used when CONFIG is unavailable. */
    const val DEFAULT_DATABASE_COUNT: Int = 16
    const val COMMAND_LIMIT: Int = 6

    /** Commands counted as reads for the secondary throughput line. */
    val readCommands: Set<String> =
        setOf(
            "get",
            "mget",
            "getrange",
            "strlen",
            "exists",
            "type",
            "ttl",
            "pttl",
            "hget",
            "hmget",
            "hgetall",
            "hexists",
            "hkeys",
            "hvals",
            "hlen",
            "lrange",
            "lindex",
            "llen",
            "smembers",
            "sismember",
            "scard",
            "zrange",
            "zrevrange",
            "zscore",
            "zcard",
            "zcount",
            "scan",
            "keys",
            "dbsize",
            "info",
            "memory",
            "slowlog",
            "client",
            "ping",
        )

    /**
     * Keeps [range] worth of samples, measured back from the newest sample.
     *
     * Using the newest sample (not the wall clock) means a paused page still shows the
     * samples it already collected.
     */
    fun filterSamples(
        samples: List<MetricsSample>,
        range: InsightsRange,
    ): List<MetricsSample> {
        if (samples.isEmpty()) return emptyList()
        if (range == InsightsRange.SESSION) return samples
        val reference = samples.last().atEpochMillis
        val cutoff = reference - range.seconds * 1_000L
        return samples.filter { it.atEpochMillis >= cutoff }
    }

    /** Drops samples outside the one-hour retention window and caps the list at 720. */
    fun pruneSamples(
        samples: MutableList<MetricsSample>,
        nowEpochMillis: Long,
    ) {
        val cutoff = nowEpochMillis - SAMPLE_RETENTION_MILLIS
        while (samples.isNotEmpty() && samples.first().atEpochMillis < cutoff) {
            samples.removeAt(0)
        }
        while (samples.size > MAX_SAMPLES) {
            samples.removeAt(0)
        }
    }

    fun hitRateOf(
        hits: Long?,
        misses: Long?,
    ): Double? {
        if (hits == null || misses == null) return null
        val total = hits + misses
        if (total <= 0L) return null
        return hits.toDouble() / total.toDouble()
    }

    fun memoryRatio(
        usedBytes: Long?,
        maxBytes: Long?,
    ): Float? {
        if (usedBytes == null || maxBytes == null || maxBytes <= 0L) return null
        return (usedBytes.toDouble() / maxBytes.toDouble()).toFloat().coerceIn(0f, 1f)
    }

    /** Reads/s from the commandstats call delta between two samples. */
    fun readOpsPerSecond(
        previous: MetricsSample?,
        current: MetricsSample,
    ): Double {
        if (previous == null) return 0.0
        val elapsedSeconds = (current.atEpochMillis - previous.atEpochMillis) / 1_000.0
        if (elapsedSeconds <= 0.0) return 0.0
        val delta =
            readCommands.sumOf { name ->
                val now = current.commandCalls[name] ?: 0L
                val before = previous.commandCalls[name] ?: 0L
                (now - before).coerceAtLeast(0L)
            }
        return delta / elapsedSeconds
    }

    /** Per-command call delta between two samples; negative deltas (counter resets) are skipped. */
    fun commandDeltas(
        previous: MetricsSample?,
        current: MetricsSample,
    ): Map<String, Long> {
        if (previous == null) return emptyMap()
        return buildMap {
            current.commandCalls.forEach { (name, calls) ->
                val delta = calls - (previous.commandCalls[name] ?: 0L)
                if (delta > 0L) put(name, delta)
            }
        }
    }

    /**
     * Top commands by session delta, plus an aggregated "其他" row.
     *
     * The first sample has no previous sample, so it deliberately yields an empty list
     * instead of pretending cumulative counters are a recent distribution.
     */
    fun commandShares(
        previous: MetricsSample?,
        current: MetricsSample?,
        limit: Int = COMMAND_LIMIT,
    ): List<CommandShare> {
        if (current == null) return emptyList()
        val deltas = commandDeltas(previous, current)
        val total = deltas.values.sum()
        if (total <= 0L) return emptyList()
        val sorted = deltas.entries.sortedWith(compareByDescending<Map.Entry<String, Long>> { it.value }.thenBy { it.key })
        val top = sorted.take(limit)
        val rest = sorted.drop(limit).sumOf { it.value }
        val shares =
            top.map { (name, calls) ->
                CommandShare(name = name.uppercase(), calls = calls, share = calls.toFloat() / total.toFloat())
            }.toMutableList()
        if (rest > 0L) {
            shares +=
                CommandShare(
                    name = "其他",
                    calls = rest,
                    share = rest.toFloat() / total.toFloat(),
                )
        }
        return shares
    }

    /** Keyspace rows for `db0..dbN`; databases missing from INFO are shown as zero. */
    fun keyspaceRows(
        section: Map<String, String>,
        databaseCount: Int = DEFAULT_DATABASE_COUNT,
    ): List<KeyspaceRow> {
        val parsed =
            section.entries.mapNotNull { (key, value) ->
                val match = DB_KEY.matchEntire(key) ?: return@mapNotNull null
                val index = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
                val fields =
                    value.split(',')
                        .mapNotNull { part ->
                            val name = part.substringBefore('=', "").trim()
                            val raw = part.substringAfter('=', "")
                            if (name.isEmpty()) null else name to raw
                        }
                        .toMap()
                index to
                    KeyspaceRow(
                        databaseIndex = index,
                        keys = fields["keys"]?.toLongOrNull() ?: 0L,
                        expires = fields["expires"]?.toLongOrNull() ?: 0L,
                        avgTtlMillis = fields["avg_ttl"]?.toLongOrNull() ?: 0L,
                    )
            }.toMap()
        return (0 until databaseCount).map { index ->
            parsed[index] ?: KeyspaceRow(databaseIndex = index, keys = 0L, expires = 0L, avgTtlMillis = 0L)
        }
    }

    fun hasKeyspace(section: Map<String, String>): Boolean = section.keys.any { DB_KEY.matches(it) }

    /**
     * Keyspace rows to render: at least [minimum] rows, extended so the last non-empty
     * database stays visible, and never more than [rows]. Servers configured with fewer
     * than [minimum] databases simply show all of them.
     */
    fun visibleKeyspaceRows(
        rows: List<KeyspaceRow>,
        minimum: Int = 5,
    ): List<KeyspaceRow> {
        if (rows.isEmpty()) return emptyList()
        val lastNonEmpty = rows.indexOfLast { it.keys > 0L }
        val visibleCount =
            (lastNonEmpty + 2)
                .coerceAtLeast(minimum)
                .coerceAtMost(rows.size)
        return rows.take(visibleCount)
    }

    /**
     * Groups clients by the prefix before `-`, `_` or `:`, e.g. `worker-17` → `worker-*`.
     * Only names and counts are exposed; client addresses stay private.
     */
    fun groupClients(
        clients: List<ClientConnection>,
        limit: Int = COMMAND_LIMIT,
    ): List<ClientGroup> {
        if (clients.isEmpty()) return emptyList()
        val counts = linkedMapOf<String, Int>()
        clients.forEach { client ->
            val group = groupName(client.name)
            counts[group] = (counts[group] ?: 0) + 1
        }
        val sorted = counts.entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        val top = sorted.take(limit)
        val rest = sorted.drop(limit).sumOf { it.value }
        val result = top.map { ClientGroup(name = it.key, count = it.value) }.toMutableList()
        if (rest > 0) {
            val existingOther = result.indexOfFirst { it.name == "other" }
            if (existingOther >= 0) {
                result[existingOther] = result[existingOther].copy(count = result[existingOther].count + rest)
            } else {
                result += ClientGroup(name = "other", count = rest)
            }
        }
        return result
    }

    private fun groupName(rawName: String): String {
        val name = rawName.trim()
        if (name.isEmpty()) return "other"
        val separator = name.indexOfFirst { it == '-' || it == '_' || it == ':' }
        return if (separator > 0) "${name.take(separator)}-*" else name
    }

    fun buildMemoryStats(snapshot: ServerSnapshot?): MemoryStats {
        if (snapshot == null) return MemoryStats()
        val memory = snapshot.sections["Memory"].orEmpty()
        val stats = snapshot.sections["Stats"].orEmpty()
        return MemoryStats(
            usedBytes = snapshot.usedMemoryBytes,
            maxBytes = snapshot.maxMemoryBytes,
            peakBytes = memory["used_memory_peak"]?.toLongOrNull(),
            fragmentation = memory["mem_fragmentation_ratio"]?.toDoubleOrNull(),
            evictedKeys = stats["evicted_keys"]?.toLongOrNull(),
            policy = memory["maxmemory_policy"]?.takeIf { it.isNotBlank() },
        )
    }

    fun clientsSummary(
        clients: List<ClientConnection>,
        snapshot: ServerSnapshot?,
    ): ClientsSummary {
        val maxClients = snapshot?.sections?.get("Clients")?.get("maxclients")?.toIntOrNull()
        val management =
            clients.count { client ->
                val name = client.name.lowercase()
                name.startsWith("redis-compose") || name.startsWith("redis-cli")
            }
        val total = clients.size.takeIf { it > 0 } ?: snapshot?.connectedClients ?: 0
        return ClientsSummary(
            connectedClients = snapshot?.connectedClients,
            maxClients = maxClients,
            appConnections = (total - management).coerceAtLeast(0),
            managementSessions = management,
            idleConnections = clients.count { it.idleSeconds >= 60L },
            groups = groupClients(clients),
            listAvailable = clients.isNotEmpty(),
        )
    }

    /** Health rows built only from INFO fields; missing data is reported as unknown, never OK. */
    fun healthItems(
        snapshot: ServerSnapshot?,
        nowEpochSeconds: Long,
    ): List<HealthItem> {
        if (snapshot == null) {
            return listOf(
                HealthItem("主从复制", "尚未读取 INFO", "未知", HealthTone.UNKNOWN),
                HealthItem("RDB 快照", "尚未读取 INFO", "未知", HealthTone.UNKNOWN),
                HealthItem("AOF 持久化", "尚未读取 INFO", "未知", HealthTone.UNKNOWN),
                HealthItem("内存水位", "尚未读取 INFO", "未知", HealthTone.UNKNOWN),
            )
        }
        val replication = snapshot.sections["Replication"].orEmpty()
        val persistence = snapshot.sections["Persistence"].orEmpty()
        val memory = snapshot.sections["Memory"].orEmpty()
        return listOf(
            replicationHealth(replication),
            rdbHealth(snapshot, persistence, nowEpochSeconds),
            aofHealth(snapshot, persistence),
            memoryHealth(snapshot, memory),
        )
    }

    private fun replicationHealth(replication: Map<String, String>): HealthItem {
        val role = replication["role"]?.lowercase()
        return when (role) {
            "master" -> {
                val replicas = replication["connected_slaves"]?.toIntOrNull()
                HealthItem(
                    title = "主从复制",
                    detail = "主节点 · ${replicas ?: "未知"} 个副本",
                    label = if (replicas != null) "正常" else "未知",
                    tone = if (replicas != null) HealthTone.OK else HealthTone.UNKNOWN,
                )
            }
            "slave" -> {
                val linked = replication["master_link_status"] == "up"
                val host = replication["master_host"]?.takeIf { it.isNotBlank() }
                HealthItem(
                    title = "主从复制",
                    detail = "从节点 · ${if (linked) "已连接主" else "主连接断开"}" + (host?.let { " · $it" } ?: ""),
                    label = if (linked) "正常" else "异常",
                    tone = if (linked) HealthTone.OK else HealthTone.DANGER,
                )
            }
            else -> HealthItem("主从复制", "INFO replication 缺少 role", "未知", HealthTone.UNKNOWN)
        }
    }

    private fun rdbHealth(
        snapshot: ServerSnapshot,
        persistence: Map<String, String>,
        nowEpochSeconds: Long,
    ): HealthItem {
        val status = persistence["rdb_last_bgsave_status"]
        val lastSave = snapshot.rdbLastSaveEpochSeconds
        return when {
            status == "err" ->
                HealthItem("RDB 快照", "最近一次后台保存失败", "异常", HealthTone.DANGER)
            lastSave != null -> {
                val age = (nowEpochSeconds - lastSave).coerceAtLeast(0L)
                HealthItem(
                    title = "RDB 快照",
                    detail = "最近保存 ${relativeSeconds(age)}",
                    label = "正常",
                    tone = HealthTone.OK,
                )
            }
            else -> HealthItem("RDB 快照", "INFO persistence 缺少保存时间", "未知", HealthTone.UNKNOWN)
        }
    }

    private fun aofHealth(
        snapshot: ServerSnapshot,
        persistence: Map<String, String>,
    ): HealthItem {
        val size = persistence["aof_current_size"]?.toLongOrNull()
        val rewriteStatus = persistence["aof_last_bgrewrite_status"]
        return when (snapshot.aofEnabled) {
            null -> HealthItem("AOF 持久化", "INFO persistence 缺少 aof_enabled", "未知", HealthTone.UNKNOWN)
            false -> HealthItem("AOF 持久化", "未开启", "未启用", HealthTone.UNKNOWN)
            true ->
                if (rewriteStatus == "err") {
                    HealthItem("AOF 持久化", "最近一次 AOF 重写失败", "异常", HealthTone.DANGER)
                } else {
                    HealthItem(
                        title = "AOF 持久化",
                        detail = "已开启" + (size?.let { " · ${formatBytes(it)}" } ?: ""),
                        label = "正常",
                        tone = HealthTone.OK,
                    )
                }
        }
    }

    private fun memoryHealth(
        snapshot: ServerSnapshot,
        memory: Map<String, String>,
    ): HealthItem {
        val max = snapshot.maxMemoryBytes
        val used = snapshot.usedMemoryBytes
        val policy = memory["maxmemory_policy"]?.takeIf { it.isNotBlank() }
        if (max == null) {
            return HealthItem("内存水位", "INFO memory 缺少 maxmemory", "未知", HealthTone.UNKNOWN)
        }
        if (max <= 0L) {
            return HealthItem("内存水位", "未设置 maxmemory · 使用 ${formatBytes(used)}", "无上限", HealthTone.UNKNOWN)
        }
        val ratio = memoryRatio(used, max) ?: return HealthItem("内存水位", "用量未知", "未知", HealthTone.UNKNOWN)
        val percent = formatPercent(ratio.toDouble())
        val detail = "${policy ?: "noeviction"} · $percent"
        return when {
            ratio >= 0.9f -> HealthItem("内存水位", detail, "偏高", HealthTone.DANGER)
            ratio >= 0.75f -> HealthItem("内存水位", detail, "关注", HealthTone.WARN)
            else -> HealthItem("内存水位", detail, "正常", HealthTone.OK)
        }
    }

    /** Rounds up to a readable chart maximum (1/2/5 × 10^n). */
    fun niceCeil(value: Double): Double {
        if (!value.isFinite() || value <= 0.0) return 1.0
        val exponent = floor(log10(value))
        val base = 10.0.pow(exponent)
        val fraction = value / base
        val nice =
            when {
                fraction <= 1.0 -> 1.0
                fraction <= 2.0 -> 2.0
                fraction <= 2.5 -> 2.5
                fraction <= 3.0 -> 3.0
                fraction <= 4.0 -> 4.0
                fraction <= 5.0 -> 5.0
                fraction <= 6.0 -> 6.0
                fraction <= 8.0 -> 8.0
                else -> 10.0
            }
        return nice * base
    }

    /** Compact axis labels such as `4.8k` / `1.2M`. */
    fun formatCompact(value: Double): String {
        if (!value.isFinite()) return "—"
        val absolute = abs(value)
        return when {
            absolute >= 1_000_000.0 -> "${oneDecimal(value / 1_000_000.0)}M"
            absolute >= 1_000.0 -> "${oneDecimal(value / 1_000.0)}k"
            else -> value.roundToLong().toString()
        }
    }

    fun formatPercent(value: Double): String = if (value.isFinite()) "${oneDecimal(value * 100.0)}%" else "—"

    /** One decimal place for ratios such as fragmentation, not compacted to an integer. */
    fun formatDecimal(value: Double?): String = value?.takeIf { it.isFinite() }?.let { oneDecimal(it) } ?: "—"

    fun formatOps(value: Double?): String = value?.takeIf { it.isFinite() }?.roundToLong()?.let { formatCount(it) } ?: "—"

    fun formatUptime(seconds: Long?): String {
        if (seconds == null || seconds < 0L) return "未知"
        val days = seconds / 86_400L
        val hours = (seconds % 86_400L) / 3_600L
        val minutes = (seconds % 3_600L) / 60L
        return when {
            days > 0L -> "$days 天 $hours 小时"
            hours > 0L -> "$hours 小时 $minutes 分"
            else -> "$minutes 分"
        }
    }

    fun relativeSeconds(seconds: Long): String =
        when {
            seconds < 60L -> "${seconds}s 前"
            seconds < 3_600L -> "${seconds / 60L}m 前"
            seconds < 86_400L -> "${seconds / 3_600L}h 前"
            else -> "${seconds / 86_400L}d 前"
        }

    fun relativeMillis(
        atEpochMillis: Long?,
        nowEpochMillis: Long,
    ): String = if (atEpochMillis == null) "等待首个样本" else relativeSeconds((nowEpochMillis - atEpochMillis).coerceAtLeast(0L) / 1_000L)

    /** Five evenly spaced samples used for X axis labels. */
    fun axisSamples(
        samples: List<MetricsSample>,
        count: Int = 5,
    ): List<MetricsSample> {
        if (samples.isEmpty() || count <= 0) return emptyList()
        if (count == 1) return listOf(samples.last())
        if (samples.size <= count) return samples
        return (0 until count).map { index ->
            samples[(index.toLong() * (samples.size - 1) / (count - 1)).toInt()]
        }
    }

    fun axisLabel(
        sample: MetricsSample,
        withSeconds: Boolean,
    ): String {
        val text = formatEpochSeconds(sample.atEpochMillis / 1_000L)
        return if (withSeconds) text else text.take(5)
    }

    private fun oneDecimal(value: Double): String {
        val rounded = (value * 10.0).roundToLong() / 10.0
        val text = rounded.toString()
        return if (text.endsWith(".0")) text.dropLast(2) else text
    }

    private val DB_KEY = Regex("""^db(\d+)$""")
}
