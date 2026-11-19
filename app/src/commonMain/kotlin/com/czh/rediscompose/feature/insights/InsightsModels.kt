package com.czh.rediscompose.feature.insights

import androidx.compose.ui.graphics.vector.ImageVector
import com.czh.rediscompose.domain.command.SlowLogEntry

enum class KpiDeltaTone { Neutral, Positive, Warning }

/** One KPI tile; formatting happens in the state so previews can supply plain strings. */
data class KpiData(
    val title: String,
    val icon: ImageVector,
    val value: String,
    val unit: String? = null,
    val delta: String? = null,
    val deltaTone: KpiDeltaTone = KpiDeltaTone.Neutral,
    val sparkline: List<Float> = emptyList(),
)

data class ThroughputData(
    val labels: List<String> = emptyList(),
    val total: List<Float> = emptyList(),
    val reads: List<Float> = emptyList(),
)

data class MemoryStats(
    val usedBytes: Long? = null,
    val maxBytes: Long? = null,
    val peakBytes: Long? = null,
    val fragmentation: Double? = null,
    val evictedKeys: Long? = null,
    val policy: String? = null,
) {
    val hasLimit: Boolean get() = (maxBytes ?: 0L) > 0L

    val usageRatio: Float?
        get() {
            val max = maxBytes ?: return null
            val used = usedBytes ?: return null
            if (max <= 0L) return null
            return (used.toDouble() / max.toDouble()).toFloat().coerceIn(0f, 1f)
        }
}

data class CommandShare(
    val name: String,
    val calls: Long,
    val share: Float,
)

data class KeyspaceRow(
    val databaseIndex: Int,
    val keys: Long,
    val expires: Long,
    val avgTtlMillis: Long,
)

data class KeyspaceData(
    val available: Boolean,
    val rows: List<KeyspaceRow> = emptyList(),
) {
    val nonEmptyCount: Int get() = rows.count { it.keys > 0L }
}

enum class HealthTone { OK, WARN, DANGER, UNKNOWN }

data class HealthItem(
    val title: String,
    val detail: String,
    val label: String,
    val tone: HealthTone,
)

data class ClientGroup(
    val name: String,
    val count: Int,
)

data class ClientsSummary(
    val connectedClients: Int? = null,
    val maxClients: Int? = null,
    val appConnections: Int = 0,
    val managementSessions: Int = 0,
    val idleConnections: Int = 0,
    val groups: List<ClientGroup> = emptyList(),
    val listAvailable: Boolean = false,
)

data class InsightsHeaderData(
    val host: String,
    val port: Int,
    val version: String? = null,
    val uptimeSeconds: Long? = null,
    val range: InsightsRange = InsightsRange.LAST_5_MINUTES,
    val autoRefresh: Boolean = true,
    val isLoading: Boolean = false,
    val lastUpdatedEpochMillis: Long? = null,
    val error: String? = null,
)

/**
 * Everything the dashboard needs for one frame.
 *
 * Built from [InsightsState] inside the feature; previews build the same object with
 * deterministic fixtures, so no composable ever connects to Redis by itself.
 */
data class InsightsDashboardData(
    val header: InsightsHeaderData,
    val kpis: List<KpiData> = emptyList(),
    val throughput: ThroughputData = ThroughputData(),
    val memory: MemoryStats = MemoryStats(),
    val commands: List<CommandShare> = emptyList(),
    val commandsSampling: Boolean = false,
    val keyspace: KeyspaceData = KeyspaceData(available = false),
    val health: List<HealthItem> = emptyList(),
    val slowLog: List<SlowLogEntry> = emptyList(),
    val clients: ClientsSummary = ClientsSummary(),
)
