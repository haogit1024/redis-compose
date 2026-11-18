package com.czh.rediscompose.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RedisCommandCatalogTest {
    @Test
    fun `suggests by uppercase prefix`() {
        val suggestions = RedisCommandCatalog.suggest("hg", limit = 6)

        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.all { it.command.startsWith("HG") })
        assertEquals("HGET", suggestions.first().command)
    }

    @Test
    fun `supports multi-word commands`() {
        assertTrue(RedisCommandCatalog.suggest("MEMORY U").any { it.command == "MEMORY USAGE" })
        assertTrue(RedisCommandCatalog.suggest("SLOWLOG G").any { it.command == "SLOWLOG GET" })
        assertTrue(RedisCommandCatalog.suggest("CLIENT L").any { it.command == "CLIENT LIST" })
    }

    @Test
    fun `respects the limit`() {
        assertEquals(2, RedisCommandCatalog.suggest("S", limit = 2).size)
    }

    @Test
    fun `blank input has no suggestions`() {
        assertTrue(RedisCommandCatalog.suggest("  ").isEmpty())
        assertTrue(RedisCommandCatalog.suggest("ZZZ").isEmpty())
    }
}
