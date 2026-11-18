package com.czh.rediscompose.data.redis

import com.czh.rediscompose.domain.command.RedisReply

/**
 * Parses `XRANGE` replies from the binary protocol:
 * `[[id, [field, value, ...]], ...]`.
 */
object StreamParser {
    fun parse(reply: RedisReply): List<StreamEntryBytes> {
        val array = reply as? RedisReply.Array ?: return emptyList()
        return array.items.mapNotNull { item ->
            val entry = item as? RedisReply.Array ?: return@mapNotNull null
            if (entry.items.size < 2) return@mapNotNull null
            val id = (entry.items[0] as? RedisReply.Bulk)?.let { ValueCodec.decode(it.value) } ?: return@mapNotNull null
            val rawFields = (entry.items[1] as? RedisReply.Array)?.items ?: return@mapNotNull null
            val pairs =
                rawFields.chunked(2).mapNotNull { pair ->
                    if (pair.size != 2) return@mapNotNull null
                    val name = (pair[0] as? RedisReply.Bulk)?.let { ValueCodec.decode(it.value) } ?: return@mapNotNull null
                    val value = (pair[1] as? RedisReply.Bulk)?.value ?: ByteArray(0)
                    name to value
                }
            StreamEntryBytes(id = id, fields = pairs)
        }
    }
}
