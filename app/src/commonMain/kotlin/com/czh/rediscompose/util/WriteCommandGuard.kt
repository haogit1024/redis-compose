package com.czh.rediscompose.util

enum class CommandSafety { READ, WRITE, UNKNOWN }

/**
 * Classifies a command for global/connection read-only mode.
 *
 * Safety wins over convenience: unknown commands are classified [CommandSafety.UNKNOWN]
 * and blocked in read-only sessions. Multi-word commands such as `CONFIG SET` and
 * `CLIENT KILL` are matched on the first two tokens.
 */
object WriteCommandGuard {
    fun isWrite(command: String): Boolean = classify(command) != CommandSafety.READ

    fun classify(command: String): CommandSafety {
        val tokens = RedisCommandParser.split(command).orEmpty()
        if (tokens.isEmpty()) return CommandSafety.UNKNOWN
        return classifyTokens(tokens)
    }

    internal fun classifyTokens(tokens: List<String>): CommandSafety {
        if (tokens.isEmpty()) return CommandSafety.UNKNOWN
        val first = tokens[0].uppercase()
        val second = tokens.getOrNull(1)?.uppercase()

        when (first) {
            "CONFIG" ->
                return when (second) {
                    "SET", "RESETSTAT" -> CommandSafety.WRITE
                    "GET" -> CommandSafety.READ
                    else -> CommandSafety.UNKNOWN
                }
            "CLIENT" ->
                return when (second) {
                    "KILL", "PAUSE", "UNPAUSE", "NO-EVICT", "NO-TOUCH" -> CommandSafety.WRITE
                    "LIST", "INFO", "GETNAME", "ID" -> CommandSafety.READ
                    else -> CommandSafety.UNKNOWN
                }
            "SCRIPT" ->
                return when (second) {
                    "LOAD", "FLUSH" -> CommandSafety.WRITE
                    "EXISTS" -> CommandSafety.READ
                    else -> CommandSafety.UNKNOWN
                }
            "ACL" ->
                return when (second) {
                    "SETUSER", "DELUSER", "LOAD", "SAVE" -> CommandSafety.WRITE
                    "WHOAMI", "LIST", "GETUSER", "CAT", "GENPASS", "LOG" -> CommandSafety.READ
                    else -> CommandSafety.UNKNOWN
                }
            "SLOWLOG" ->
                return when (second) {
                    "RESET" -> CommandSafety.WRITE
                    "GET", "LEN", "HELP" -> CommandSafety.READ
                    else -> CommandSafety.UNKNOWN
                }
            "XGROUP" -> return CommandSafety.WRITE
        }

        return when (first) {
            in WRITE_COMMANDS -> CommandSafety.WRITE
            in READ_COMMANDS -> CommandSafety.READ
            else -> CommandSafety.UNKNOWN
        }
    }

    private val WRITE_COMMANDS =
        setOf(
            "SET",
            "SETEX",
            "PSETEX",
            "SETNX",
            "GETSET",
            "GETDEL",
            "GETEX",
            "APPEND",
            "SETRANGE",
            "SETBIT",
            "MSET",
            "MSETNX",
            "INCR",
            "INCRBY",
            "INCRBYFLOAT",
            "DECR",
            "DECRBY",
            "DEL",
            "UNLINK",
            "EXPIRE",
            "EXPIREAT",
            "PEXPIRE",
            "PEXPIREAT",
            "PERSIST",
            "RENAME",
            "RENAMENX",
            "MOVE",
            "COPY",
            "RESTORE",
            "MIGRATE",
            "SWAPDB",
            "HSET",
            "HSETNX",
            "HDEL",
            "HINCRBY",
            "HINCRBYFLOAT",
            "HMSET",
            "LPUSH",
            "LPUSHX",
            "RPUSH",
            "RPUSHX",
            "LPOP",
            "RPOP",
            "LSET",
            "LREM",
            "LTRIM",
            "LINSERT",
            "RPOPLPUSH",
            "LMOVE",
            "BLMOVE",
            "BLPOP",
            "BRPOP",
            "SADD",
            "SREM",
            "SPOP",
            "SMOVE",
            "SDIFFSTORE",
            "SINTERSTORE",
            "SUNIONSTORE",
            "ZADD",
            "ZREM",
            "ZINCRBY",
            "ZREMRANGEBYRANK",
            "ZREMRANGEBYSCORE",
            "ZREMRANGEBYLEX",
            "ZPOPMIN",
            "ZPOPMAX",
            "BZPOPMIN",
            "BZPOPMAX",
            "PFADD",
            "PFMERGE",
            "GEOADD",
            "XADD",
            "XDEL",
            "XTRIM",
            "XACK",
            "XCLAIM",
            "XGROUP",
            "FLUSHDB",
            "FLUSHALL",
            "EVAL",
            "EVALSHA",
            "MULTI",
            "EXEC",
            "DISCARD",
        )

    private val READ_COMMANDS =
        setOf(
            "PING",
            "ECHO",
            "INFO",
            "DBSIZE",
            "TIME",
            "LASTSAVE",
            "GET",
            "MGET",
            "STRLEN",
            "GETRANGE",
            "EXISTS",
            "TYPE",
            "TTL",
            "PTTL",
            "SCAN",
            "KEYS",
            "HGET",
            "HMGET",
            "HGETALL",
            "HLEN",
            "HEXISTS",
            "HKEYS",
            "HVALS",
            "HSCAN",
            "LRANGE",
            "LLEN",
            "LINDEX",
            "SMEMBERS",
            "SCARD",
            "SISMEMBER",
            "SRANDMEMBER",
            "SSCAN",
            "ZCARD",
            "ZCOUNT",
            "ZSCORE",
            "ZRANGE",
            "ZREVRANGE",
            "ZRANGEBYSCORE",
            "ZRANK",
            "ZREVRANK",
            "ZSCAN",
            "XRANGE",
            "XREVRANGE",
            "XLEN",
            "XINFO",
            "XREAD",
            "MEMORY",
            "OBJECT",
            "SLOWLOG",
            "COMMAND",
            "SELECT",
            "PUBLISH",
            "DUMP",
            "LATENCY",
            "HELP",
            "PFCOUNT",
            "GEOSEARCH",
            "GEOPOS",
            "GEODIST",
        )
}
