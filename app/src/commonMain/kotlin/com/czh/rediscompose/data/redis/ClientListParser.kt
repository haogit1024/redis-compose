package com.czh.rediscompose.data.redis

import com.czh.rediscompose.domain.command.ClientConnection

/**
 * Parses the text returned by `CLIENT LIST`.
 *
 * Each line contains `key=value` pairs; `name=` may be empty and is therefore captured
 * with a nullable whitespace-free value.
 */
object ClientListParser {
    fun parse(raw: String): List<ClientConnection> =
        raw.lineSequence().mapNotNull { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@mapNotNull null
            val fields =
                FIELD.findAll(line)
                    .associate { it.groupValues[1] to it.groupValues[2] }
            val id = fields["id"] ?: return@mapNotNull null
            val address = fields["addr"] ?: return@mapNotNull null
            ClientConnection(
                id = id,
                address = address,
                name = fields["name"].orEmpty(),
                database = fields["db"]?.toIntOrNull() ?: 0,
                idleSeconds = fields["idle"]?.toLongOrNull() ?: 0L,
            )
        }.toList()

    private val FIELD = Regex("""(\w+)=(\S*)""")
}
