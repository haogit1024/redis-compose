package com.czh.rediscompose.data.redis

import com.czh.rediscompose.domain.command.CommandStat
import com.czh.rediscompose.domain.key.DatabaseInfo
import com.czh.rediscompose.domain.server.ServerSnapshot

/**
 * Parser for `INFO` and `INFO commandstats` output.
 *
 * Values are kept as strings in [parse]; [snapshot] only converts the fields the UI
 * needs. A missing field becomes `null`, while `0` stays a real value.
 */
object InfoParser {
    fun parse(raw: String): Map<String, Map<String, String>> {
        val sections = linkedMapOf<String, LinkedHashMap<String, String>>()
        var currentSection = ""
        raw.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.isEmpty() -> Unit
                line.startsWith("#") -> {
                    currentSection = line.trimStart('#').trim()
                    sections.getOrPut(currentSection) { linkedMapOf() }
                }
                ':' in line -> {
                    val key = line.substringBefore(':').trim()
                    val value = line.substringAfter(':')
                    if (key.isNotEmpty()) {
                        val target = sections.getOrPut(currentSection) { linkedMapOf() }
                        target[key] = value
                    }
                }
            }
        }
        return sections
    }

    fun snapshot(raw: String): ServerSnapshot {
        val sections = parse(raw)
        val server = sections["Server"].orEmpty()
        val memory = sections["Memory"].orEmpty()
        val clients = sections["Clients"].orEmpty()
        val stats = sections["Stats"].orEmpty()
        val replication = sections["Replication"].orEmpty()
        val persistence = sections["Persistence"].orEmpty()
        val keyspace = sections["Keyspace"].orEmpty()
        val databases = parseKeyspace(keyspace)
        return ServerSnapshot(
            sections = sections,
            version = server["redis_version"]?.takeIf { it.isNotBlank() },
            uptimeSeconds = server["uptime_in_seconds"]?.toLongOrNull(),
            usedMemoryBytes = memory["used_memory"]?.toLongOrNull(),
            maxMemoryBytes = memory["maxmemory"]?.toLongOrNull(),
            connectedClients = clients["connected_clients"]?.toIntOrNull(),
            totalKeys =
                if (sections.containsKey("Keyspace")) {
                    databases.sumOf { it.keyCount }
                } else {
                    null
                },
            keyspaceHits = stats["keyspace_hits"]?.toLongOrNull(),
            keyspaceMisses = stats["keyspace_misses"]?.toLongOrNull(),
            totalCommandsProcessed = stats["total_commands_processed"]?.toLongOrNull(),
            instantaneousOpsPerSecond = stats["instantaneous_ops_per_second"]?.toDoubleOrNull(),
            databases = databases,
            replicationRole = replication["role"]?.takeIf { it.isNotBlank() },
            rdbLastSaveEpochSeconds = persistence["rdb_last_save_time"]?.toLongOrNull(),
            aofEnabled =
                when (persistence["aof_enabled"]) {
                    "1" -> true
                    "0" -> false
                    else -> null
                },
        )
    }

    fun parseKeyspace(section: Map<String, String>): List<DatabaseInfo> =
        section.entries.mapNotNull { (key, value) ->
            val match = DB_KEY.matchEntire(key) ?: return@mapNotNull null
            val index = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
            val count =
                value.split(',')
                    .mapNotNull { part ->
                        val name = part.substringBefore('=', "").trim()
                        val rawValue = part.substringAfter('=', "")
                        if (name == "keys") rawValue.toLongOrNull() else null
                    }
                    .firstOrNull()
            if (count == null) null else DatabaseInfo(index = index, keyCount = count)
        }.sortedBy { it.index }

    fun commandStats(raw: String): List<CommandStat> =
        raw.lineSequence().mapNotNull { rawLine ->
            val line = rawLine.trim()
            if (!line.startsWith("cmdstat_")) return@mapNotNull null
            val name = line.substringAfter("cmdstat_").substringBefore(':').trim()
            if (name.isEmpty()) return@mapNotNull null
            val values =
                line.substringAfter(':', "")
                    .split(',')
                    .mapNotNull { part ->
                        val key = part.substringBefore('=', "").trim()
                        val value = part.substringAfter('=', "").trim()
                        if (key.isEmpty()) null else key to value
                    }
                    .toMap()
            CommandStat(
                name = name,
                calls = values["calls"]?.toLongOrNull() ?: 0L,
                totalUsec = values["usec"]?.toLongOrNull() ?: 0L,
                usecPerCall = values["usec_per_call"]?.toDoubleOrNull() ?: 0.0,
            )
        }.sortedWith(compareByDescending<CommandStat> { it.calls }.thenBy { it.name }).toList()

    private val DB_KEY = Regex("""^db(\d+)$""")
}
