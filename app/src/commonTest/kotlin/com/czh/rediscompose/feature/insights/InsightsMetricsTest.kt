package com.czh.rediscompose.feature.insights

import com.czh.rediscompose.domain.command.ClientConnection
import com.czh.rediscompose.domain.key.DatabaseInfo
import com.czh.rediscompose.domain.server.MetricsSample
import com.czh.rediscompose.domain.server.ServerSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InsightsMetricsTest {
    private fun sample(
        at: Long,
        calls: Map<String, Long> = emptyMap(),
        ops: Double = 10.0,
    ) = MetricsSample(
        atEpochMillis = at,
        usedMemoryBytes = 1_000L,
        opsPerSecond = ops,
        hitRate = 0.9,
        connectedClients = 3,
        totalCommands = 100L,
        commandCalls = calls,
    )

    private fun snapshot(
        sections: Map<String, Map<String, String>>,
        usedMemory: Long? = 1_000L,
        maxMemory: Long? = 2_000L,
        aofEnabled: Boolean? = false,
        rdbLastSave: Long? = 1_000L,
        role: String? = "master",
    ) = ServerSnapshot(
        sections = sections,
        version = "7.4.2",
        uptimeSeconds = 100L,
        usedMemoryBytes = usedMemory,
        maxMemoryBytes = maxMemory,
        connectedClients = 3,
        totalKeys = 10L,
        keyspaceHits = 90L,
        keyspaceMisses = 10L,
        totalCommandsProcessed = 100L,
        instantaneousOpsPerSecond = 12.0,
        databases = listOf(DatabaseInfo(0, 10)),
        replicationRole = role,
        rdbLastSaveEpochSeconds = rdbLastSave,
        aofEnabled = aofEnabled,
    )

    @Test
    fun `range filtering keeps the requested window measured from the newest sample`() {
        val samples = (0 until 10).map { index -> sample(at = index * 60_000L) }

        val fiveMinutes = InsightsMetrics.filterSamples(samples, InsightsRange.LAST_5_MINUTES)
        val session = InsightsMetrics.filterSamples(samples, InsightsRange.SESSION)

        assertEquals(6, fiveMinutes.size)
        assertEquals(samples.last(), fiveMinutes.last())
        assertEquals(10, session.size)
    }

    @Test
    fun `pruning drops old samples and keeps at most 720`() {
        val now = 10_000_000L
        val samples =
            (0..800).map { index ->
                sample(at = now - (800 - index) * 5_000L)
            }.toMutableList()

        InsightsMetrics.pruneSamples(samples, now)

        assertEquals(720, samples.size)
        assertTrue(samples.first().atEpochMillis >= now - InsightsMetrics.SAMPLE_RETENTION_MILLIS)
    }

    @Test
    fun `hit rate and memory ratio return null for missing denominators`() {
        assertEquals(0.9, InsightsMetrics.hitRateOf(90, 10))
        assertNull(InsightsMetrics.hitRateOf(null, 10))
        assertNull(InsightsMetrics.hitRateOf(0, 0))
        assertEquals(0.5f, InsightsMetrics.memoryRatio(100, 200))
        assertNull(InsightsMetrics.memoryRatio(100, 0))
        assertNull(InsightsMetrics.memoryRatio(null, 200))
    }

    @Test
    fun `read ops uses only the read command delta over elapsed seconds`() {
        val previous = sample(at = 0, calls = mapOf("get" to 100, "set" to 50))
        val current = sample(at = 2_000, calls = mapOf("get" to 140, "set" to 90, "hgetall" to 20))

        val reads = InsightsMetrics.readOpsPerSecond(previous, current)

        assertEquals(30.0, reads)
    }

    @Test
    fun `command shares ignore the first sample and aggregate the rest`() {
        val previous =
            sample(
                at = 0,
                calls = mapOf("get" to 10, "set" to 10, "hgetall" to 10, "scan" to 10, "zrange" to 10, "del" to 10, "expire" to 10),
            )
        val current =
            sample(
                at = 5_000,
                calls =
                    mapOf(
                        "get" to 60,
                        "set" to 20,
                        "hgetall" to 20,
                        "scan" to 20,
                        "zrange" to 20,
                        "del" to 20,
                        "expire" to 15,
                    ),
            )

        val shares = InsightsMetrics.commandShares(previous, current)

        assertEquals(7, shares.size)
        assertEquals("GET", shares.first().name)
        assertEquals("其他", shares.last().name)
        assertEquals(1.0f, shares.sumOf { it.share.toDouble() }.toFloat(), 0.001f)
        assertTrue(InsightsMetrics.commandShares(null, current).isEmpty())
    }

    @Test
    fun `keyspace rows include empty databases and average ttl`() {
        val section =
            mapOf(
                "db0" to "keys=10,expires=2,avg_ttl=5000",
                "db3" to "keys=1,expires=0,avg_ttl=0",
            )

        val rows = InsightsMetrics.keyspaceRows(section)

        assertEquals(InsightsMetrics.DEFAULT_DATABASE_COUNT, rows.size)
        assertEquals(10L, rows[0].keys)
        assertEquals(5_000L, rows[0].avgTtlMillis)
        assertEquals(1L, rows[3].keys)
        assertEquals(0L, rows[1].keys)
        assertTrue(InsightsMetrics.hasKeyspace(section))
        assertTrue(!InsightsMetrics.hasKeyspace(mapOf("Keyspace" to "")))
    }

    @Test
    fun `keyspace rows stop at the server database count`() {
        val section = mapOf("db0" to "keys=10,expires=0,avg_ttl=0")

        val rows = InsightsMetrics.keyspaceRows(section, databaseCount = 4)

        assertEquals(4, rows.size)
        assertEquals((0..3).toList(), rows.map { it.databaseIndex })
    }

    @Test
    fun `visible keyspace rows never exceed the server database count`() {
        val rows = InsightsMetrics.keyspaceRows(mapOf("db0" to "keys=10,expires=0,avg_ttl=0"), databaseCount = 4)

        assertEquals(4, InsightsMetrics.visibleKeyspaceRows(rows).size)
        assertEquals(emptyList(), InsightsMetrics.visibleKeyspaceRows(emptyList()))
    }

    @Test
    fun `visible keyspace rows keep five and cover the last non-empty database`() {
        val compact = InsightsMetrics.keyspaceRows(mapOf("db0" to "keys=10,expires=0,avg_ttl=0"))
        assertEquals(5, InsightsMetrics.visibleKeyspaceRows(compact).size)

        val spread =
            InsightsMetrics.keyspaceRows(
                mapOf(
                    "db0" to "keys=10,expires=0,avg_ttl=0",
                    "db10" to "keys=1,expires=0,avg_ttl=0",
                ),
            )
        assertEquals(12, InsightsMetrics.visibleKeyspaceRows(spread).size)
    }

    @Test
    fun `clients group by name prefix and keep addresses out`() {
        val clients =
            (1..3).map { ClientConnection(id = "$it", address = "10.0.0.$it", name = "worker-$it", database = 0, idleSeconds = 10) } +
                listOf(
                    ClientConnection("x", "10.0.0.9", "redis-compose", 0, 5),
                    ClientConnection("y", "10.0.0.8", "", 0, 120),
                )

        val groups = InsightsMetrics.groupClients(clients)
        val summary = InsightsMetrics.clientsSummary(clients, snapshot(emptyMap()))

        assertEquals(ClientGroup("worker-*", 3), groups.first())
        assertTrue(groups.any { it.name == "other" })
        assertEquals(4, summary.appConnections)
        assertEquals(1, summary.managementSessions)
        assertEquals(1, summary.idleConnections)
        assertTrue(summary.listAvailable)
    }

    @Test
    fun `health rows report unknown instead of healthy when fields are missing`() {
        val unknown = InsightsMetrics.healthItems(null, nowEpochSeconds = 10_000L)
        assertTrue(unknown.all { it.tone == HealthTone.UNKNOWN })

        val master =
            InsightsMetrics.healthItems(
                snapshot(
                    sections =
                        mapOf(
                            "Replication" to mapOf("role" to "master", "connected_slaves" to "2"),
                            "Persistence" to mapOf("rdb_last_bgsave_status" to "ok", "aof_enabled" to "0"),
                            "Memory" to mapOf("maxmemory_policy" to "allkeys-lru"),
                        ),
                    rdbLastSave = 9_000L,
                ),
                nowEpochSeconds = 10_000L,
            )
        assertEquals(HealthTone.OK, master[0].tone)
        assertTrue(master[0].detail.contains("2 个副本"))
        assertEquals(HealthTone.OK, master[1].tone)
        assertTrue(master[2].label == "未启用")
        assertEquals(HealthTone.OK, master[3].tone)
        assertEquals("50%", master[3].detail.substringAfter(" · "))
    }

    @Test
    fun `memory health warns when no maxmemory is configured`() {
        val items =
            InsightsMetrics.healthItems(
                snapshot(
                    sections = mapOf("Memory" to mapOf("maxmemory_policy" to "noeviction")),
                    maxMemory = 0L,
                ),
                nowEpochSeconds = 10_000L,
            )

        val memory = items.last()
        assertEquals("无上限", memory.label)
        assertEquals(HealthTone.UNKNOWN, memory.tone)
        assertTrue(memory.detail.contains("未设置 maxmemory"))
    }

    @Test
    fun `replication errors are reported as danger`() {
        val items =
            InsightsMetrics.healthItems(
                snapshot(
                    sections =
                        mapOf(
                            "Replication" to
                                mapOf(
                                    "role" to "slave",
                                    "master_link_status" to "down",
                                    "master_host" to "10.0.0.1",
                                ),
                        ),
                ),
                nowEpochSeconds = 10_000L,
            )

        assertEquals(HealthTone.DANGER, items.first().tone)
        assertEquals("异常", items.first().label)
    }

    @Test
    fun `chart helpers produce readable maxima and compact labels`() {
        assertEquals(10.0, InsightsMetrics.niceCeil(9.1))
        assertEquals(20.0, InsightsMetrics.niceCeil(11.0))
        assertEquals(6_000.0, InsightsMetrics.niceCeil(5_462.0))
        assertEquals(1.0, InsightsMetrics.niceCeil(0.0))
        assertEquals("1.2k", InsightsMetrics.formatCompact(1_234.0))
        assertEquals("4.8k", InsightsMetrics.formatCompact(4_750.0))
        assertEquals("5", InsightsMetrics.formatCompact(4.75))
        assertEquals("1.1", InsightsMetrics.formatDecimal(1.11))
        assertEquals("2 天 3 小时", InsightsMetrics.formatUptime(2 * 86_400L + 3 * 3_600L))
        assertEquals("45s 前", InsightsMetrics.relativeSeconds(45L))
        assertEquals("等待首个样本", InsightsMetrics.relativeMillis(null, 0L))
    }

    @Test
    fun `memory stats parse peak fragmentation and evictions`() {
        val stats =
            InsightsMetrics.buildMemoryStats(
                snapshot(
                    sections =
                        mapOf(
                            "Memory" to
                                mapOf(
                                    "used_memory_peak" to "1800",
                                    "mem_fragmentation_ratio" to "1.11",
                                    "maxmemory_policy" to "allkeys-lru",
                                ),
                            "Stats" to mapOf("evicted_keys" to "42"),
                        ),
                ),
            )

        assertEquals(1_800L, stats.peakBytes)
        assertEquals(1.11, stats.fragmentation)
        assertEquals(42L, stats.evictedKeys)
        assertEquals("allkeys-lru", stats.policy)
        assertNotNull(stats.usageRatio)
    }
}
