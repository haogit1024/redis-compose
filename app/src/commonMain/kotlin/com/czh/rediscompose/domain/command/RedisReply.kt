package com.czh.rediscompose.domain.command

/**
 * Recursive Redis command reply.
 *
 * Arrays keep their nesting; a nil element is [Nil], never a null bulk. Bulk payloads
 * stay as bytes so non-UTF-8 values can be rendered as HEX.
 */
sealed interface RedisReply {
    data class Simple(val value: String) : RedisReply

    data class Error(val message: String) : RedisReply

    data class Integer(val value: Long) : RedisReply

    data class Bulk(val value: ByteArray) : RedisReply

    data class Array(val items: List<RedisReply>) : RedisReply

    data object Nil : RedisReply
}
