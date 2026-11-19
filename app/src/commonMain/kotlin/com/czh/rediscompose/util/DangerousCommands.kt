package com.czh.rediscompose.util

/**
 * Commands that need a confirmation step when `confirmDangerousCommands` is enabled.
 *
 * The list is intentionally explicit: unknown or read-only commands never require a
 * confirmation, and `CONFIG GET` stays allowed while `CONFIG SET` does not.
 */
object DangerousCommands {
    private val singleTokenCommands =
        setOf(
            "FLUSHALL",
            "FLUSHDB",
            "SHUTDOWN",
            "SWAPDB",
            "DEBUG",
            "MIGRATE",
            "RESTORE",
            "REPLICAOF",
            "SLAVEOF",
            "MONITOR",
        )

    private val multiTokenCommands =
        setOf(
            "CONFIG SET",
            "CONFIG RESETSTAT",
            "CONFIG REWRITE",
            "SCRIPT FLUSH",
            "SCRIPT KILL",
            "FUNCTION FLUSH",
            "FUNCTION DELETE",
            "FUNCTION RESTORE",
            "FUNCTION LOAD",
            "ACL SETUSER",
            "ACL DELUSER",
            "CLIENT KILL",
        )

    private val evalCommands =
        setOf("EVAL", "EVALSHA", "EVAL_RO", "EVALSHA_RO", "FCALL", "FCALL_RO")

    fun isDangerous(tokens: List<String>): Boolean {
        if (tokens.isEmpty()) return false
        val first = tokens[0].uppercase()
        val second = tokens.getOrNull(1)?.uppercase()
        if (first in singleTokenCommands || first in evalCommands) return true
        return second != null && "$first $second" in multiTokenCommands
    }

    fun isDangerous(command: String): Boolean {
        val tokens = RedisCommandParser.split(command) ?: return false
        return isDangerous(tokens)
    }
}
