package com.czh.rediscompose.data.redis

import com.czh.rediscompose.domain.command.RedisReply

/**
 * Converts the raw object returned by Jedis' protocol reader into [RedisReply].
 *
 * The nesting of multi-bulk replies is preserved; byte payloads stay untouched.
 */
object RedisReplyParser {
    fun fromRaw(raw: Any?): RedisReply =
        when (raw) {
            null -> RedisReply.Nil
            is ByteArray -> RedisReply.Bulk(raw)
            is Long -> RedisReply.Integer(raw)
            is Int -> RedisReply.Integer(raw.toLong())
            is String -> RedisReply.Simple(raw)
            is List<*> -> RedisReply.Array(raw.map { fromRaw(it) })
            else -> RedisReply.Simple(raw.toString())
        }
}
