package com.czh.redis

import com.czh.redis.redis.ValueCodec
import com.czh.redis.redis.buildKeyTree
import com.czh.redis.redis.parseInfoSections
import com.czh.redis.ui.parseArgs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SharedCommonTest {
    @Test
    fun keyTree_buildsDirectories() {
        val tree =
            buildKeyTree(
                listOf("user:1001:name", "user:1001:age", "user:1002:name", "order:1"),
                ":",
            )
        val roots = tree.map { it.name }.toSet()
        assertEquals(setOf("user", "order"), roots)
        val user = tree.first { it.name == "user" }
        assertTrue(user.isDirectory)
        assertEquals(listOf("1001", "1002"), user.children.map { it.name })
        assertEquals(3, user.count)
    }

    @Test
    fun keyTree_noSeparatorFlattens() {
        val tree = buildKeyTree(listOf("a", "b", "a"), "")
        assertEquals(listOf("a", "b"), tree.map { it.name })
        assertTrue(tree.all { !it.isDirectory })
    }

    @Test
    fun keyTree_duplicateLeavesAreCollapsed() {
        val tree = buildKeyTree(listOf("k:v", "k:v"), ":")
        assertEquals(1, tree.first { it.isDirectory }.count)
    }

    @Test
    fun valueCodec_decodeAndHex() {
        assertEquals("hello", ValueCodec.decode("hello".encodeToByteArray()))
        assertEquals(null, ValueCodec.decode(byteArrayOf(0xFF.toByte(), 0x00.toByte())))
        assertEquals("FF 00", ValueCodec.hex(byteArrayOf(0xFF.toByte(), 0x00.toByte())))
    }

    @Test
    fun valueCodec_jsonDetection() {
        assertTrue(ValueCodec.isJson("""{"a": 1}"""))
        assertFalse(ValueCodec.isJson("not json"))
    }

    @Test
    fun parseInfoSections_splitsSections() {
        val info =
            """
            # Server
            redis_version:7.0.0
            os:macos

            # Memory
            used_memory:1024
            """.trimIndent()
        val sections = parseInfoSections(info)
        assertEquals(listOf("Server", "Memory"), sections.map { it.first })
        assertEquals("7.0.0", sections.first().second.first { it.first == "redis_version" }.second)
    }

    @Test
    fun parseArgs_handlesQuotes() {
        assertEquals(listOf("SET", "key", "hello world"), parseArgs("SET key \"hello world\""))
        assertEquals(listOf("GET", "k"), parseArgs("  GET   k  "))
    }
}
