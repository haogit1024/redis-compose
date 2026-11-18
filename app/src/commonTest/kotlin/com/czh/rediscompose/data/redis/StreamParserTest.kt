package com.czh.rediscompose.data.redis

import com.czh.rediscompose.domain.command.RedisReply
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StreamParserTest {
    @Test
    fun `parses range entries and keeps field bytes`() {
        val reply =
            RedisReply.Array(
                listOf(
                    RedisReply.Array(
                        listOf(
                            RedisReply.Bulk("1700000000000-0".encodeToByteArray()),
                            RedisReply.Array(
                                listOf(
                                    RedisReply.Bulk("event".encodeToByteArray()),
                                    RedisReply.Bulk("checkout".encodeToByteArray()),
                                    RedisReply.Bulk("attempt".encodeToByteArray()),
                                    RedisReply.Bulk("1".encodeToByteArray()),
                                ),
                            ),
                        ),
                    ),
                ),
            )
        val entry = StreamParser.parse(reply).single()
        assertEquals("1700000000000-0", entry.id)
        assertEquals(2, entry.fields.size)
        assertEquals("event", entry.fields[0].first)
        assertEquals("checkout", entry.fields[0].second.decodeToString())
        assertEquals("attempt", entry.fields[1].first)
    }

    @Test
    fun `malformed replies return empty`() {
        assertTrue(StreamParser.parse(RedisReply.Nil).isEmpty())
        assertTrue(
            StreamParser.parse(RedisReply.Array(listOf(RedisReply.Bulk("bad".encodeToByteArray())))).isEmpty(),
        )
    }
}
