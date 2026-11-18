package com.czh.rediscompose.domain.command

data class SlowLogEntry(
    val id: Long,
    val timestampEpochSeconds: Long,
    val durationMicros: Long,
    val command: List<String>,
    val clientAddress: String?,
    val clientName: String?,
)
