package com.czh.rediscompose.data.redis

import com.czh.rediscompose.domain.command.RedisReply
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RedisReplyParserTest {
    @Test
    fun `null becomes nil`() {
        assertEquals(RedisReply.Nil, RedisReplyParser.fromRaw(null))
    }

    @Test
    fun `integers become integer replies`() {
        assertEquals(RedisReply.Integer(42), RedisReplyParser.fromRaw(42L))
        assertEquals(RedisReply.Integer(7), RedisReplyParser.fromRaw(7))
    }

    @Test
    fun `bytes become bulk replies`() {
        val reply = RedisReplyParser.fromRaw("héllo".encodeToByteArray())
        val bulk = assertIs<RedisReply.Bulk>(reply)
        assertEquals("héllo", bulk.value.decodeToString())
    }

    @Test
    fun `strings become simple replies`() {
        assertEquals(RedisReply.Simple("PONG"), RedisReplyParser.fromRaw("PONG"))
    }

    @Test
    fun `nested arrays keep their structure`() {
        val reply =
            RedisReplyParser.fromRaw(
                listOf(
                    "a".encodeToByteArray(),
                    listOf(1L, null, "b".encodeToByteArray()),
                ),
            )
        val array = assertIs<RedisReply.Array>(reply)
        assertEquals(2, array.items.size)
        assertContentEquals("a".encodeToByteArray(), (array.items[0] as RedisReply.Bulk).value)
        val inner = assertIs<RedisReply.Array>(array.items[1])
        assertEquals(RedisReply.Integer(1), inner.items[0])
        assertEquals(RedisReply.Nil, inner.items[1])
        assertContentEquals("b".encodeToByteArray(), (inner.items[2] as RedisReply.Bulk).value)
    }

    @Test
    fun `empty list is an empty array not nil`() {
        val reply = RedisReplyParser.fromRaw(emptyList<Any>())
        val array = assertIs<RedisReply.Array>(reply)
        assertTrue(array.items.isEmpty())
    }
}
