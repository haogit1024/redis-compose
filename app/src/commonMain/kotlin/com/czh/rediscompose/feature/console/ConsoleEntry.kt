package com.czh.rediscompose.feature.console

import com.czh.rediscompose.domain.command.RedisReply

/** One line in the terminal log. */
sealed interface ConsoleEntry {
    data class Command(
        val at: String,
        val text: String,
        val promptHost: String = "",
    ) : ConsoleEntry

    data class Note(val text: String) : ConsoleEntry

    data class Output(
        val reply: RedisReply,
        val durationMillis: Long,
    ) : ConsoleEntry

    data class Error(val message: String) : ConsoleEntry
}

/** Row shown in the right-hand history pane. */
data class ConsoleHistoryItem(
    val command: String,
    val at: String,
    val durationMillis: Long?,
    val summary: String,
    val failed: Boolean = false,
)
