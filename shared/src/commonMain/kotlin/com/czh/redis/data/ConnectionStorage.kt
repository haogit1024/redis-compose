package com.czh.redis.data

import kotlinx.serialization.builtins.ListSerializer

expect fun loadConnections(): List<RedisConnection>

expect fun saveConnections(connections: List<RedisConnection>)

internal val connectionListSerializer = ListSerializer(RedisConnection.serializer())
