package com.czh.rediscompose.data.redis

/**
 * User-facing data-layer failure, already mapped from Jedis exceptions.
 *
 * `message` must be safe to show in a toast; detailed causes stay in [cause].
 */
class RedisException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
