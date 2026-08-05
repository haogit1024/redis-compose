package com.czh.redis.redis

import com.czh.redis.data.RedisConnection

expect fun createRedisClient(connection: RedisConnection): RedisClient
