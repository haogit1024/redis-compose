package com.czh.rediscompose.data.jedis

import com.czh.rediscompose.data.redis.RedisGateway
import com.czh.rediscompose.data.redis.RedisGatewayFactory
import com.czh.rediscompose.domain.connection.ConnectionProfile

class JedisGatewayFactory : RedisGatewayFactory {
    override fun create(profile: ConnectionProfile): RedisGateway = JedisRedisGateway(profile)
}
