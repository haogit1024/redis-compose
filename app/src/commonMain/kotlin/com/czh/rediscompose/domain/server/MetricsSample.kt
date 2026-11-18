package com.czh.rediscompose.domain.server

data class MetricsSample(
    val atEpochMillis: Long,
    val usedMemoryBytes: Long,
    val opsPerSecond: Double,
    val hitRate: Double,
    val connectedClients: Int,
    val totalCommands: Long,
    val commandCalls: Map<String, Long>,
)
