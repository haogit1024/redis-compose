package com.czh.rediscompose.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WriteCommandGuardTest {
    @Test
    fun `blocks common write commands`() {
        listOf(
            "SET key value",
            "DEL key",
            "FLUSHDB",
            "FLUSHALL",
            "HSET h f v",
            "LPUSH l a",
            "SADD s a",
            "ZADD z 1 a",
            "EXPIRE k 10",
            "RENAME a b",
            "EVAL \"return 1\" 0",
            "MULTI",
        ).forEach { command ->
            assertTrue(WriteCommandGuard.isWrite(command), command)
            assertEquals(CommandSafety.WRITE, WriteCommandGuard.classify(command), command)
        }
    }

    @Test
    fun `classifies multi-word admin commands`() {
        assertTrue(WriteCommandGuard.isWrite("CONFIG SET maxmemory 1gb"))
        assertFalse(WriteCommandGuard.isWrite("CONFIG GET maxmemory"))
        assertTrue(WriteCommandGuard.isWrite("CLIENT KILL ID 3"))
        assertFalse(WriteCommandGuard.isWrite("CLIENT LIST"))
        assertTrue(WriteCommandGuard.isWrite("SCRIPT LOAD \"return 1\""))
        assertFalse(WriteCommandGuard.isWrite("SCRIPT EXISTS abc"))
        assertTrue(WriteCommandGuard.isWrite("SLOWLOG RESET"))
        assertFalse(WriteCommandGuard.isWrite("SLOWLOG GET 10"))
    }

    @Test
    fun `allows read-only commands`() {
        listOf(
            "PING",
            "INFO memory",
            "DBSIZE",
            "GET key",
            "HGETALL h",
            "SCAN 0 MATCH user:* COUNT 100",
            "TTL key",
            "MEMORY USAGE key",
            "OBJECT ENCODING key",
            "CLIENT LIST",
            "SELECT 1",
            "DUMP key",
            "PUBLISH channel message",
        ).forEach { command ->
            assertFalse(WriteCommandGuard.isWrite(command), command)
            assertEquals(CommandSafety.READ, WriteCommandGuard.classify(command), command)
        }
    }

    @Test
    fun `unknown commands are unknown and blocked in read-only mode`() {
        assertEquals(CommandSafety.UNKNOWN, WriteCommandGuard.classify("FOOBAR key"))
        assertTrue(WriteCommandGuard.isWrite("FOOBAR key"))
        assertEquals(CommandSafety.UNKNOWN, WriteCommandGuard.classify("CONFIG WEIRD"))
    }
}
