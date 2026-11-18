package com.czh.rediscompose.domain.key

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KeyTreeBuilderTest {
    @Test
    fun `build returns empty for empty input`() {
        assertEquals(emptyList(), KeyTreeBuilder.build(emptyList(), ":"))
    }

    @Test
    fun `build nests paths and counts leaves`() {
        val nodes =
            KeyTreeBuilder.build(
                listOf(
                    "app:config:flags",
                    "app:config:limits",
                    "app:session:abc",
                    "cache:user:1",
                    "zebra",
                ),
                ":",
            )
        assertEquals(listOf("app", "cache", "zebra"), nodes.map { it.name })
        assertTrue(nodes[0].isDirectory)
        assertEquals(3, nodes[0].count)
        assertEquals(emptyList(), nodes[2].children)
        assertFalse(nodes[2].isDirectory)

        val appChildren = nodes[0].children
        assertEquals(listOf("config", "session"), appChildren.map { it.name })
        assertEquals(2, appChildren[0].count)
        assertEquals(1, appChildren[1].count)
        assertEquals(listOf("flags", "limits"), appChildren[0].children.map { it.name })
        assertEquals("app:config:flags", appChildren[0].children[0].path)
        assertEquals("app:config:flags", appChildren[0].children[0].key?.name)
    }

    @Test
    fun `directories sort before keys and names are case insensitive`() {
        val nodes =
            KeyTreeBuilder.build(
                listOf("Beta:1", "alpha", "Zeta:2", "apple:3"),
                ":",
            )
        assertEquals(listOf("apple", "Beta", "Zeta", "alpha"), nodes.map { it.name })
        assertEquals(listOf(true, true, true, false), nodes.map { it.isDirectory })
    }

    @Test
    fun `empty separator produces a flat list`() {
        val nodes = KeyTreeBuilder.build(listOf("a:b", "c", "a:b"), "")
        assertEquals(listOf("a:b", "c"), nodes.map { it.name })
        assertTrue(nodes.all { !it.isDirectory && it.count == 1 })
        assertEquals(KeyType.NONE, nodes[0].key?.type)
        assertEquals("a:b", nodes[0].path)
    }

    @Test
    fun `single key without separator is a leaf`() {
        val nodes = KeyTreeBuilder.build(listOf("solo"), ":")
        assertEquals(1, nodes.size)
        assertEquals("solo", nodes[0].name)
        assertEquals("solo", nodes[0].path)
        assertEquals(KeyType.NONE, nodes[0].key?.type)
    }

    @Test
    fun `single key with separator creates one directory`() {
        val nodes = KeyTreeBuilder.build(listOf("solo:key"), ":")
        assertEquals(listOf("solo"), nodes.map { it.name })
        assertEquals(1, nodes[0].children.size)
        assertEquals("solo:key", nodes[0].children[0].path)
    }

    @Test
    fun `duplicate keys are counted once`() {
        val nodes = KeyTreeBuilder.build(listOf("a:1", "a:1", "a:2"), ":")
        assertEquals(2, nodes[0].count)
    }
}
