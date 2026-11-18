package com.czh.rediscompose.util

data class RedisCommandSuggestion(
    val command: String,
    val syntax: String,
)

/**
 * Small local catalog used for the composer's autocomplete.
 *
 * This is intentionally not a complete Redis manual: it lists common commands with enough
 * syntax to jog memory, and suggestions never send anything by themselves.
 */
object RedisCommandCatalog {
    val commands: List<RedisCommandSuggestion> =
        listOf(
            suggestion("PING", ""),
            suggestion("ECHO", "message"),
            suggestion("INFO", "[section]"),
            suggestion("DBSIZE", ""),
            suggestion("GET", "key"),
            suggestion("SET", "key value [EX seconds] [NX|XX]"),
            suggestion("DEL", "key [key …]"),
            suggestion("EXPIRE", "key seconds"),
            suggestion("PERSIST", "key"),
            suggestion("INCR", "key"),
            suggestion("DECR", "key"),
            suggestion("HSET", "key field value [field value …]"),
            suggestion("HDEL", "key field [field …]"),
            suggestion("LPUSH", "key element [element …]"),
            suggestion("RPUSH", "key element [element …]"),
            suggestion("SADD", "key member [member …]"),
            suggestion("SREM", "key member [member …]"),
            suggestion("ZADD", "key score member [score member …]"),
            suggestion("ZREM", "key member [member …]"),
            suggestion("RENAME", "key newkey"),
            suggestion("MGET", "key [key …]"),
            suggestion("STRLEN", "key"),
            suggestion("GETRANGE", "key start end"),
            suggestion("TYPE", "key"),
            suggestion("TTL", "key"),
            suggestion("PTTL", "key"),
            suggestion("EXISTS", "key [key …]"),
            suggestion("SCAN", "cursor [MATCH pattern] [COUNT n] [TYPE type]"),
            suggestion("KEYS", "pattern"),
            suggestion("HGET", "key field"),
            suggestion("HMGET", "key field [field …]"),
            suggestion("HGETALL", "key"),
            suggestion("HLEN", "key"),
            suggestion("HSCAN", "key cursor [MATCH pattern] [COUNT n]"),
            suggestion("LRANGE", "key start stop"),
            suggestion("LLEN", "key"),
            suggestion("LINDEX", "key index"),
            suggestion("SMEMBERS", "key"),
            suggestion("SCARD", "key"),
            suggestion("SISMEMBER", "key member"),
            suggestion("SSCAN", "key cursor [MATCH pattern] [COUNT n]"),
            suggestion("ZRANGE", "key start stop [WITHSCORES]"),
            suggestion("ZREVRANGE", "key start stop [WITHSCORES]"),
            suggestion("ZCARD", "key"),
            suggestion("ZSCORE", "key member"),
            suggestion("ZSCAN", "key cursor [MATCH pattern] [COUNT n]"),
            suggestion("MEMORY USAGE", "key [SAMPLES n]"),
            suggestion("MEMORY DOCTOR", ""),
            suggestion("SLOWLOG GET", "[count]"),
            suggestion("CLIENT LIST", "[TYPE normal]"),
            suggestion("XINFO GROUPS", "key"),
            suggestion("CONFIG GET", "pattern"),
            suggestion("SCRIPT EXISTS", "sha1 [sha1 …]"),
            suggestion("XRANGE", "key start end [COUNT n]"),
            suggestion("XREVRANGE", "key end start [COUNT n]"),
            suggestion("XLEN", "key"),
            suggestion("MEMORY", "USAGE key [SAMPLES n] | DOCTOR | STATS"),
            suggestion("OBJECT", "ENCODING key"),
            suggestion("SLOWLOG", "GET [count] | LEN | RESET"),
            suggestion("CLIENT", "LIST [TYPE normal] | INFO <id>"),
            suggestion("CONFIG", "GET pattern"),
            suggestion("COMMAND", "COUNT | INFO [command …]"),
            suggestion("SELECT", "index"),
            suggestion("PUBLISH", "channel message"),
            suggestion("DUMP", "key"),
            suggestion("MULTI", ""),
            suggestion("EXEC", ""),
            suggestion("DISCARD", ""),
            suggestion("HELP", "[command]"),
        )

    /** Suggestions whose command starts with the typed prefix. */
    fun suggest(
        input: String,
        limit: Int = 6,
    ): List<RedisCommandSuggestion> {
        val query = input.trim().uppercase()
        if (query.isEmpty()) return emptyList()
        return commands
            .filter { suggestion -> suggestion.command.startsWith(query) }
            .take(limit.coerceAtLeast(0))
    }

    private fun suggestion(
        command: String,
        syntax: String,
    ): RedisCommandSuggestion = RedisCommandSuggestion(command = command, syntax = syntax)
}
