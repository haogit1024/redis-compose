package com.czh.rediscompose.data.redis

import com.czh.rediscompose.domain.command.RedisReply
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SlowLogParserTest {
    private fun bulk(text: String): RedisReply = RedisReply.Bulk(text.encodeToByteArray())

    @Test
    fun `parses client metadata when present`() {
        val reply =
            RedisReply.Array(
                listOf(
                    RedisReply.Array(
                        listOf(
                            RedisReply.Integer(7),
                            RedisReply.Integer(1_700_000_000),
                            RedisReply.Integer(250),
                            RedisReply.Array(listOf(bulk("GET"), bulk("cache:user:1"))),
                            bulk("10.0.0.1:5123"),
                            bulk("worker"),
                        ),
                    ),
                ),
            )
        val entries = SlowLogParser.parse(reply)
        assertEquals(1, entries.size)
        val entry = entries.single()
        assertEquals(7, entry.id)
        assertEquals(1_700_000_000, entry.timestampEpochSeconds)
        assertEquals(250, entry.durationMicros)
        assertEquals(listOf("GET", "cache:user:1"), entry.command)
        assertEquals("10.0.0.1:5123", entry.clientAddress)
        assertEquals("worker", entry.clientName)
    }

    @Test
    fun `older entries without client fields still parse`() {
        val reply =
            RedisReply.Array(
                listOf(
                    RedisReply.Array(
                        listOf(
                            RedisReply.Integer(1),
                            RedisReply.Integer(100),
                            RedisReply.Integer(5),
                            RedisReply.Array(listOf(bulk("PING"))),
                        ),
                    ),
                ),
            )
        val entry = SlowLogParser.parse(reply).single()
        assertNull(entry.clientAddress)
        assertNull(entry.clientName)
    }

    @Test
    fun `malformed entries and non-array replies are ignored`() {
        val reply =
            RedisReply.Array(
                listOf(
                    bulk("nope"),
                    RedisReply.Array(listOf(RedisReply.Integer(1))),
                ),
            )
        assertTrue(SlowLogParser.parse(reply).isEmpty())
        assertTrue(SlowLogParser.parse(bulk("nope")).isEmpty())
    }
}
