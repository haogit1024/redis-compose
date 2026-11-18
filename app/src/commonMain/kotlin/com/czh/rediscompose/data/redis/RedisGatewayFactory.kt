package com.czh.rediscompose.data.redis

import com.czh.rediscompose.domain.connection.ConnectionProfile

fun interface RedisGatewayFactory {
    fun create(profile: ConnectionProfile): RedisGateway
}
