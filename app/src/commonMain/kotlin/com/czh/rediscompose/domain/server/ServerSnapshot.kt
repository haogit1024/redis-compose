package com.czh.rediscompose.domain.server

import com.czh.rediscompose.domain.key.DatabaseInfo

/**
 * Parsed `INFO` response with the raw sections kept for diagnostics.
 *
 * Missing fields stay `null`; zero is kept as a real value.
 */
data class ServerSnapshot(
    val sections: Map<String, Map<String, String>>,
    val version: String?,
    val uptimeSeconds: Long?,
    val usedMemoryBytes: Long?,
    val maxMemoryBytes: Long?,
    val connectedClients: Int?,
    val totalKeys: Long?,
    val keyspaceHits: Long?,
    val keyspaceMisses: Long?,
    val totalCommandsProcessed: Long?,
    val instantaneousOpsPerSecond: Double?,
    val databases: List<DatabaseInfo>,
    val replicationRole: String?,
    val rdbLastSaveEpochSeconds: Long?,
    val aofEnabled: Boolean?,
)
