package com.czh.rediscompose.feature.console

import com.czh.rediscompose.data.redis.ValueCodec
import com.czh.rediscompose.domain.command.RedisReply

enum class ConsoleOutputTone { Normal, Success, Dim, Error }

data class ConsoleOutputLine(
    val text: String,
    val indent: Int = 0,
    val tone: ConsoleOutputTone = ConsoleOutputTone.Normal,
)

data class ConsoleReplyView(
    val headline: String,
    val lines: List<ConsoleOutputLine>,
)

/** Converts recursive Redis replies into terminal lines; pure and unit-testable. */
object ConsoleOutput {
    fun view(reply: RedisReply): ConsoleReplyView =
        when (reply) {
            is RedisReply.Simple ->
                ConsoleReplyView(
                    headline = "simple",
                    lines = listOf(ConsoleOutputLine(reply.value, tone = ConsoleOutputTone.Success)),
                )
            is RedisReply.Integer ->
                ConsoleReplyView(
                    headline = "integer",
                    lines =
                        listOf(
                            ConsoleOutputLine("(integer) ${reply.value}", tone = ConsoleOutputTone.Success),
                        ),
                )
            is RedisReply.Bulk -> {
                val decoded = ValueCodec.decode(reply.value)
                when {
                    decoded == null ->
                        ConsoleReplyView(
                            headline = "hex",
                            lines =
                                listOf(
                                    ConsoleOutputLine(
                                        "(hex) ${hexPreview(ValueCodec.hex(reply.value))}",
                                        tone = ConsoleOutputTone.Dim,
                                    ),
                                ),
                        )
                    decoded.contains('\n') -> {
                        val textLines = decoded.lines()
                        val shown = textLines.take(MAX_BULK_LINES).map { ConsoleOutputLine(it) }.toMutableList()
                        if (textLines.size > MAX_BULK_LINES) {
                            shown +=
                                ConsoleOutputLine(
                                    "… 已截断 ${textLines.size - MAX_BULK_LINES} 行",
                                    tone = ConsoleOutputTone.Dim,
                                )
                        }
                        ConsoleReplyView(
                            headline = "bulk · ${textLines.size} lines",
                            lines = shown,
                        )
                    }
                    else ->
                        ConsoleReplyView(
                            headline = "bulk",
                            lines = listOf(ConsoleOutputLine(quoted(decoded), tone = ConsoleOutputTone.Success)),
                        )
                }
            }
            RedisReply.Nil ->
                ConsoleReplyView(
                    headline = "nil",
                    lines = listOf(ConsoleOutputLine("(nil)", tone = ConsoleOutputTone.Dim)),
                )
            is RedisReply.Error ->
                ConsoleReplyView(
                    headline = "error",
                    lines = listOf(ConsoleOutputLine("(error) ${reply.message}", tone = ConsoleOutputTone.Error)),
                )
            is RedisReply.Array ->
                if (reply.items.isEmpty()) {
                    ConsoleReplyView(
                        headline = "array · 0 items",
                        lines = listOf(ConsoleOutputLine("(empty array)", tone = ConsoleOutputTone.Dim)),
                    )
                } else {
                    ConsoleReplyView(
                        headline = "array · ${reply.items.size} items",
                        lines = arrayLines(reply.items, depth = 0, indent = 0),
                    )
                }
        }

    /** Plain text used by "复制全部" and future export. */
    fun plainText(reply: RedisReply): String =
        view(reply).lines.joinToString("\n") { line ->
            " ".repeat(line.indent * 2) + line.text
        }

    fun summary(reply: RedisReply): String =
        when (reply) {
            is RedisReply.Simple -> "simple"
            is RedisReply.Integer -> "integer"
            is RedisReply.Bulk -> {
                val decoded = ValueCodec.decode(reply.value)
                when {
                    decoded == null -> "hex"
                    decoded.contains('\n') -> "bulk · ${decoded.lines().size} lines"
                    else -> "bulk"
                }
            }
            RedisReply.Nil -> "nil"
            is RedisReply.Error -> "error"
            is RedisReply.Array -> "array · ${reply.items.size} items"
        }

    private fun arrayLines(
        items: List<RedisReply>,
        depth: Int,
        indent: Int,
    ): List<ConsoleOutputLine> {
        if (depth > MAX_DEPTH) {
            return listOf(ConsoleOutputLine("… 已截断深层嵌套", indent, ConsoleOutputTone.Dim))
        }
        val lines = mutableListOf<ConsoleOutputLine>()
        val shownItems = items.take(MAX_ITEMS)
        for ((index, item) in shownItems.withIndex()) {
            if (lines.size >= MAX_OUTPUT_LINES) break
            val prefix = "${index + 1}) "
            if (item is RedisReply.Array) {
                val label = if (item.items.isEmpty()) "(empty array)" else "array(${item.items.size})"
                lines += ConsoleOutputLine("$prefix$label", indent, ConsoleOutputTone.Dim)
                if (item.items.isNotEmpty()) {
                    lines += arrayLines(item.items, depth + 1, indent + 1)
                }
            } else {
                val scalar = scalarLines(item)
                scalar.forEachIndexed { lineIndex, line ->
                    lines +=
                        line.copy(
                            text = if (lineIndex == 0) prefix + line.text else line.text,
                            indent = if (lineIndex == 0) indent else indent + 1,
                        )
                }
            }
        }
        if (lines.size >= MAX_OUTPUT_LINES) {
            lines += ConsoleOutputLine("… 输出过长，已截断", indent, ConsoleOutputTone.Dim)
        } else if (items.size > MAX_ITEMS) {
            lines +=
                ConsoleOutputLine(
                    "… 已截断 ${items.size - MAX_ITEMS} 项",
                    indent,
                    ConsoleOutputTone.Dim,
                )
        }
        return lines
    }

    private fun scalarLines(reply: RedisReply): List<ConsoleOutputLine> =
        when (reply) {
            is RedisReply.Simple -> listOf(ConsoleOutputLine(reply.value, tone = ConsoleOutputTone.Success))
            is RedisReply.Integer -> listOf(ConsoleOutputLine("(integer) ${reply.value}", tone = ConsoleOutputTone.Success))
            is RedisReply.Bulk -> {
                val decoded = ValueCodec.decode(reply.value)
                when {
                    decoded == null ->
                        listOf(ConsoleOutputLine("(hex) ${hexPreview(ValueCodec.hex(reply.value))}", tone = ConsoleOutputTone.Dim))
                    decoded.contains('\n') -> decoded.lines().map { ConsoleOutputLine(it) }
                    else -> listOf(ConsoleOutputLine(quoted(decoded), tone = ConsoleOutputTone.Success))
                }
            }
            RedisReply.Nil -> listOf(ConsoleOutputLine("(nil)", tone = ConsoleOutputTone.Dim))
            is RedisReply.Error -> listOf(ConsoleOutputLine("(error) ${reply.message}", tone = ConsoleOutputTone.Error))
            is RedisReply.Array -> listOf(ConsoleOutputLine("array(${reply.items.size})", tone = ConsoleOutputTone.Dim))
        }

    private fun quoted(text: String): String = "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private fun hexPreview(hex: String): String = if (hex.length <= MAX_HEX_CHARS) hex else hex.take(MAX_HEX_CHARS) + " …"

    private const val MAX_DEPTH = 3
    private const val MAX_ITEMS = 200
    private const val MAX_BULK_LINES = 300
    private const val MAX_OUTPUT_LINES = 600
    private const val MAX_HEX_CHARS = 2_048
}
