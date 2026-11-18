package com.czh.rediscompose.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RedisCommandParserTest {
    @Test
    fun `splits plain commands on whitespace`() {
        assertEquals(listOf("GET", "key:1"), RedisCommandParser.split("  GET   key:1  "))
    }

    @Test
    fun `keeps spaces inside double quotes`() {
        assertEquals(
            listOf("SET", "user:1", "Ada Lovelace"),
            RedisCommandParser.split("""SET user:1 "Ada Lovelace""""),
        )
    }

    @Test
    fun `keeps spaces inside single quotes`() {
        assertEquals(
            listOf("ECHO", "hello world"),
            RedisCommandParser.split("ECHO 'hello world'"),
        )
    }

    @Test
    fun `supports escaped quotes and backslashes`() {
        assertEquals(
            listOf("SET", "k", "a\"b", "c\\d", "line\nnext"),
            RedisCommandParser.split("""SET k "a\"b" "c\\d" "line\nnext""""),
        )
    }

    @Test
    fun `concatenates quoted and unquoted fragments`() {
        assertEquals(listOf("prefix-suffix"), RedisCommandParser.split("""pre"fix"-suffix"""))
    }

    @Test
    fun `keeps an explicitly empty argument`() {
        assertEquals(listOf("SET", "k", ""), RedisCommandParser.split("""SET k """""))
    }

    @Test
    fun `returns null for an unterminated quote`() {
        assertNull(RedisCommandParser.split("""SET k "unfinished"""))
        assertNull(RedisCommandParser.split("ECHO 'unfinished"))
    }

    @Test
    fun `blank input has no tokens`() {
        assertEquals(emptyList(), RedisCommandParser.split("   "))
    }
}
