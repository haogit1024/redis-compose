package com.czh.rediscompose.domain.connection

interface ConnectionRepository {
    suspend fun load(): List<ConnectionProfile>

    suspend fun save(connections: List<ConnectionProfile>)
}
