package com.czh.rediscompose.domain.command

data class ClientConnection(
    val id: String,
    val address: String,
    val name: String,
    val database: Int,
    val idleSeconds: Long,
)
