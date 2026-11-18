package com.czh.rediscompose.data.redis

import com.czh.rediscompose.domain.command.RedisReply
import com.czh.rediscompose.domain.command.SlowLogEntry

/**
 * Parses the nested array returned by `SLOWLOG GET n`.
 *
 * Redis 6+ entries have client address/name at indexes 4/5; older servers only return
 * the first four fields, which stay `null`.
 */
object SlowLogParser {
    fun parse(reply: RedisReply): List<SlowLogEntry> {
        val array = reply as? RedisReply.Array ?: return emptyList()
        return array.items.mapNotNull { item -> parseEntry(item) }
    }

    private fun parseEntry(item: RedisReply): SlowLogEntry? {
        val entry = item as? RedisReply.Array ?: return null
        if (entry.items.size < 4) return null
        val id = entry.items.integerAt(0) ?: return null
        val timestamp = entry.items.integerAt(1) ?: return null
        val duration = entry.items.integerAt(2) ?: return null
        val command =
            (entry.items[3] as? RedisReply.Array)
                ?.items
                ?.mapNotNull { bulk -> bulk.text() }
                .orEmpty()
        return SlowLogEntry(
            id = id,
            timestampEpochSeconds = timestamp,
            durationMicros = duration,
            command = command,
            clientAddress = entry.items.getOrNull(4)?.text()?.takeIf { it.isNotEmpty() },
            clientName = entry.items.getOrNull(5)?.text()?.takeIf { it.isNotEmpty() },
        )
    }

    private fun List<RedisReply>.integerAt(index: Int): Long? = (getOrNull(index) as? RedisReply.Integer)?.value

    private fun RedisReply.text(): String? =
        when (this) {
            is RedisReply.Bulk -> ValueCodec.decode(value) ?: ValueCodec.hex(value)
            is RedisReply.Simple -> value
            else -> null
        }
}
