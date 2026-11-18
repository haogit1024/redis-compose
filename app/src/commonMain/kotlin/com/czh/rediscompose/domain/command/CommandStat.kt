package com.czh.rediscompose.domain.command

data class CommandStat(
    val name: String,
    val calls: Long,
    val totalUsec: Long,
    val usecPerCall: Double,
)
