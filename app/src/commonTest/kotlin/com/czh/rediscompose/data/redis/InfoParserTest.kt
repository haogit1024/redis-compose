package com.czh.rediscompose.data.redis

import com.czh.rediscompose.domain.key.DatabaseInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class InfoParserTest {
    private val rawInfo =
        """
        # Server
        redis_version:7.4.2
        uptime_in_seconds:12345

        # Memory
        used_memory:1331439861
        maxmemory:2147483648
        used_memory_human:1.24G

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
        aof_current_size:0

        # Keyspace
        db0:keys=12480,expires=30,avg_ttl=1000
        db2:keys=3900,expires=0,avg_ttl=0
        """.trimIndent()

    @Test
    fun `parse groups values into sections`() {
        val sections = InfoParser.parse(rawInfo)
        assertEquals("7.4.2", sections.getValue("Server")["redis_version"])
        assertEquals("1.24G", sections.getValue("Memory")["used_memory_human"])
        assertEquals("db0:keys=12480,expires=30,avg_ttl=1000", "db0:${sections.getValue("Keyspace")["db0"]}")
    }

    @Test
    fun `parse keeps colons inside values`() {
        val sections = InfoParser.parse("# Server\nconfig_file:/etc/redis/redis.conf\n")
        assertEquals("/etc/redis/redis.conf", sections.getValue("Server")["config_file"])
    }

    @Test
    fun `snapshot maps server and memory fields`() {
        val snapshot = InfoParser.snapshot(rawInfo)
        assertEquals("7.4.2", snapshot.version)
        assertEquals(12345L, snapshot.uptimeSeconds)
        assertEquals(1331439861L, snapshot.usedMemoryBytes)
        assertEquals(2147483648L, snapshot.maxMemoryBytes)
        assertEquals(24, snapshot.connectedClients)
        assertEquals("master", snapshot.replicationRole)
        assertEquals(1700000000L, snapshot.rdbLastSaveEpochSeconds)
        assertEquals(false, snapshot.aofEnabled)
        assertEquals(12.5, snapshot.instantaneousOpsPerSecond)
        assertEquals(900L, snapshot.totalCommandsProcessed)
        assertEquals(100L, snapshot.keyspaceHits)
        assertEquals(4L, snapshot.keyspaceMisses)
    }

    @Test
    fun `snapshot sums keyspace and lists databases`() {
        val snapshot = InfoParser.snapshot(rawInfo)
        assertEquals(16380L, snapshot.totalKeys)
        assertEquals(
            listOf(DatabaseInfo(0, 12480L), DatabaseInfo(2, 3900L)),
            snapshot.databases,
        )
    }

    @Test
    fun `snapshot uses null for missing fields`() {
        val snapshot = InfoParser.snapshot("# Server\nredis_version:7.0.0\n")
        assertEquals("7.0.0", snapshot.version)
        assertNull(snapshot.usedMemoryBytes)
        assertNull(snapshot.maxMemoryBytes)
        assertNull(snapshot.connectedClients)
        assertNull(snapshot.totalKeys)
        assertNull(snapshot.aofEnabled)
        assertNull(snapshot.rdbLastSaveEpochSeconds)
        assertEquals(emptyList(), snapshot.databases)
    }

    @Test
    fun `snapshot keeps zero as a real value`() {
        val snapshot = InfoParser.snapshot("# Memory\nused_memory:0\nmaxmemory:0\n# Keyspace\ndb0:keys=0,expires=0,avg_ttl=0\n")
        assertEquals(0L, snapshot.usedMemoryBytes)
        assertEquals(0L, snapshot.maxMemoryBytes)
        assertEquals(0L, snapshot.totalKeys)
    }

    @Test
    fun `parseKeyspace ignores malformed entries`() {
        val databases =
            InfoParser.parseKeyspace(
                mapOf(
                    "db1" to "keys=5,expires=1,avg_ttl=0",
                    "dbX" to "keys=9",
                    "db2" to "expires=0",
                ),
            )
        assertEquals(listOf(DatabaseInfo(1, 5L)), databases)
    }

    @Test
    fun `commandStats parses and sorts by calls`() {
        val raw =
            """
            # Commandstats
            cmdstat_get:calls=10,usec=100,usec_per_call=10.00,rejected_calls=0,failed_calls=0
            cmdstat_set:calls=50,usec=250,usec_per_call=5.00
            cmdstat_info:calls=2,usec=20,usec_per_call=10.00
            """.trimIndent()
        val stats = InfoParser.commandStats(raw)
        assertEquals(listOf("set", "get", "info"), stats.map { it.name })
        assertEquals(50L, stats[0].calls)
        assertEquals(5.0, stats[0].usecPerCall)
        assertEquals(100L, stats[1].totalUsec)
    }

    @Test
    fun `commandStats handles empty input`() {
        assertEquals(emptyList(), InfoParser.commandStats(""))
    }

    @Test
    fun `aof enabled one is true`() {
        val snapshot = InfoParser.snapshot("# Persistence\naof_enabled:1\n")
        assertEquals(true, snapshot.aofEnabled)
        assertFalse(snapshot.sections.isEmpty())
    }
}
