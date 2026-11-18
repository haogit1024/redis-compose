package com.czh.rediscompose.feature.console

import com.czh.rediscompose.domain.command.RedisReply
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConsoleOutputTest {
    @Test
    fun `renders simple integer nil and errors`() {
        assertEquals("PONG", ConsoleOutput.view(RedisReply.Simple("PONG")).lines.single().text)
        assertEquals("(integer) 42", ConsoleOutput.view(RedisReply.Integer(42)).lines.single().text)
        assertEquals("(nil)", ConsoleOutput.view(RedisReply.Nil).lines.single().text)
        assertEquals(
            "(error) ERR bad thing",
            ConsoleOutput.view(RedisReply.Error("ERR bad thing")).lines.single().text,
        )
    }

    @Test
    fun `renders utf8 bulk with quotes and multiline without quotes`() {
        val single = ConsoleOutput.view(RedisReply.Bulk("hello".encodeToByteArray()))
        assertEquals("\"hello\"", single.lines.single().text)

        val multiline = ConsoleOutput.view(RedisReply.Bulk("# Memory\nused_memory:1\n".encodeToByteArray()))
        assertEquals(3, multiline.lines.size)
        assertEquals("# Memory", multiline.lines.first().text)
        assertEquals("bulk · 3 lines", multiline.headline)
    }

    @Test
    fun `renders non utf8 bulk as hex`() {
        val view = ConsoleOutput.view(RedisReply.Bulk(byteArrayOf(0xFF.toByte(), 0xFE.toByte())))

        assertTrue(view.lines.single().text.startsWith("(hex)"))
        assertTrue(view.lines.single().text.contains("FF FE"))
        assertEquals("hex", view.headline)
    }

    @Test
    fun `renders nested arrays with indentation`() {
        val reply =
            RedisReply.Array(
                listOf(
                    RedisReply.Bulk("0".encodeToByteArray()),
                    RedisReply.Array(
                        listOf(
                            RedisReply.Bulk("queue:emails".encodeToByteArray()),
                            RedisReply.Bulk("queue:jobs".encodeToByteArray()),
                        ),
                    ),
                ),
            )
        val lines = ConsoleOutput.view(reply).lines

        assertEquals("1) \"0\"", lines[0].text)
        assertEquals("2) array(2)", lines[1].text)
        assertEquals("1) \"queue:emails\"", lines[2].text)
        assertEquals(1, lines[2].indent)
        assertEquals("2) \"queue:jobs\"", lines[3].text)
    }

    @Test
    fun `truncates very large arrays with a note`() {
        val reply = RedisReply.Array((0..204).map { RedisReply.Integer(it.toLong()) })
        val view = ConsoleOutput.view(reply)

        assertEquals("array · 205 items", view.headline)
        assertTrue(view.lines.any { it.text.contains("已截断 5 项") })
    }

    @Test
    fun `plain text keeps prefixes and indentation`() {
        val text =
            ConsoleOutput.plainText(
                RedisReply.Array(listOf(RedisReply.Integer(1), RedisReply.Array(listOf(RedisReply.Bulk("a".encodeToByteArray()))))),
            )

        assertTrue(text.contains("1) (integer) 1"))
        assertTrue(text.contains("2) array(1)"))
        assertTrue(text.contains("  1) \"a\""))
    }
}
