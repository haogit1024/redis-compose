package com.czh.redis.redis

import com.czh.redis.data.RedisConnection

actual fun createRedisClient(connection: RedisConnection): RedisClient = JedisRedisClient(connection)
