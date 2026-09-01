package com.czh.redis.redis

import com.czh.redis.data.RedisConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import redis.clients.jedis.CommandArguments
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.Jedis
import redis.clients.jedis.commands.ProtocolCommand
import redis.clients.jedis.params.ScanParams

class JedisRedisClient(private val connection: RedisConnection) : RedisClient {
    @Volatile
    private var jedis: Jedis? = null

    private val mutex = Mutex()

    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { mutex.withLock { block() } }

    private suspend fun ioUnit(block: () -> Unit) = withContext(Dispatchers.IO) { mutex.withLock { block() } }

    private fun client(): Jedis {
        jedis?.let { return it }
        synchronized(this) {
            jedis?.let { return it }
            val port = connection.port.toIntOrNull() ?: 6379
            val builder =
                DefaultJedisClientConfig.builder()
                    .connectionTimeoutMillis(connection.connectTimeoutMs.toInt())
                    .socketTimeoutMillis(connection.readTimeoutMs.toInt())
                    .ssl(connection.ssl)
                    .database(connection.defaultDb)
            if (connection.username.isNotEmpty()) {
                builder.user(connection.username).password(connection.password)
            } else if (connection.password.isNotEmpty()) {
                builder.password(connection.password)
            }
            val j = Jedis(HostAndPort(connection.host, port), builder.build())
            jedis = j
            return j
        }
    }

    override suspend fun ping(): String = io { client().ping() }

    override suspend fun info(section: String?): String =
        io {
            if (section.isNullOrBlank()) client().info() else client().info(section)
        }

    override suspend fun select(db: Int) = ioUnit { client().select(db) }

    override suspend fun dbsize(): Long = io { client().dbSize() }

    override suspend fun scan(
        cursor: Long,
        pattern: String?,
        count: Int,
    ): ScanResult =
        io {
            val params = ScanParams().count(count)
            if (!pattern.isNullOrEmpty()) params.match(pattern)
            val res = client().scan(cursor.toString(), params)
            ScanResult(res.getCursor().toLongOrNull() ?: 0L, res.getResult())
        }

    override suspend fun type(key: String): RedisType =
        io {
            RedisType.fromLabel(client().type(key))
        }

    override suspend fun ttl(key: String): Long = io { client().ttl(key) }

    override suspend fun memoryUsage(key: String): Long? =
        io {
            client().memoryUsage(key.toByteArray())
        }

    override suspend fun getString(key: String): ByteArray? =
        io {
            client().get(key.toByteArray())
        }

    override suspend fun setString(
        key: String,
        value: ByteArray,
    ) = ioUnit {
        client().set(key.toByteArray(), value)
    }

    override suspend fun getHash(key: String): List<Pair<String, ByteArray>> =
        io {
            client().hgetAll(key.toByteArray()).map { Pair(ValueCodec.decode(it.key) ?: "", it.value) }
        }

    override suspend fun hSet(
        key: String,
        field: String,
        value: ByteArray,
    ) = ioUnit {
        client().hset(key.toByteArray(), field.toByteArray(), value)
    }

    override suspend fun hDel(
        key: String,
        field: String,
    ) = ioUnit {
        client().hdel(key.toByteArray(), field.toByteArray())
    }

    override suspend fun getList(key: String): List<ByteArray> =
        io {
            client().lrange(key.toByteArray(), 0, -1)
        }

    override suspend fun listPush(
        key: String,
        value: ByteArray,
        head: Boolean,
    ) = ioUnit {
        if (head) client().lpush(key.toByteArray(), value) else client().rpush(key.toByteArray(), value)
    }

    override suspend fun listSet(
        key: String,
        index: Long,
        value: ByteArray,
    ) = ioUnit {
        client().lset(key.toByteArray(), index, value)
    }

    override suspend fun listRemove(
        key: String,
        value: ByteArray,
    ) = ioUnit {
        client().lrem(key.toByteArray(), 0, value)
    }

    override suspend fun getSet(key: String): List<ByteArray> =
        io {
            client().smembers(key.toByteArray()).toList()
        }

    override suspend fun setAdd(
        key: String,
        value: ByteArray,
    ) = ioUnit {
        client().sadd(key.toByteArray(), value)
    }

    override suspend fun setRemove(
        key: String,
        value: ByteArray,
    ) = ioUnit {
        client().srem(key.toByteArray(), value)
    }

    override suspend fun getZSet(key: String): List<ZSetEntry> =
        io {
            client().zrangeWithScores(key.toByteArray(), 0, -1).map { ZSetEntry(it.getBinaryElement(), it.getScore()) }
        }

    override suspend fun zAdd(
        key: String,
        member: ByteArray,
        score: Double,
    ) = ioUnit {
        client().zadd(key.toByteArray(), score, member)
    }

    override suspend fun zRemove(
        key: String,
        member: ByteArray,
    ) = ioUnit {
        client().zrem(key.toByteArray(), member)
    }

    override suspend fun del(key: String) = ioUnit { client().del(key) }

    override suspend fun rename(
        key: String,
        newKey: String,
    ) = ioUnit {
        client().rename(key, newKey)
    }

    override suspend fun expire(
        key: String,
        ttl: Long,
    ) = ioUnit {
        client().expire(key, ttl)
    }

    override suspend fun persist(key: String) = ioUnit { client().persist(key) }

    override suspend fun command(args: List<String>): CommandResult =
        io {
            if (args.isEmpty()) return@io CommandResult.Error("命令为空")
            val name = args.first().uppercase()
            try {
                val cmdArgs = CommandArguments(NamedCommand(name))
                for (arg in args.drop(1)) cmdArgs.add(arg)
                val reply = client().getConnection().executeCommand(cmdArgs)
                formatReply(reply)
            } catch (e: Exception) {
                CommandResult.Error(e.message ?: e.javaClass.simpleName)
            }
        }

    override suspend fun close() =
        io {
            runCatching { jedis?.close() }
            jedis = null
        }

    private fun formatReply(reply: Any?): CommandResult =
        when (reply) {
            null -> CommandResult.Nil
            is ByteArray -> CommandResult.Bulk(reply)
            is Long -> CommandResult.Integer(reply)
            is String -> CommandResult.Status(reply)
            is List<*> -> CommandResult.Array(reply.mapNotNull { formatValue(it) })
            else -> CommandResult.Status(reply.toString())
        }

    private fun formatValue(value: Any?): String =
        when (value) {
            is ByteArray -> ValueCodec.decode(value) ?: ValueCodec.hex(value)
            is List<*> -> value.joinToString(", ") { formatValue(it) }
            null -> "nil"
            else -> value.toString()
        }
}

private class NamedCommand(private val name: String) : ProtocolCommand {
    override fun getRaw(): ByteArray = name.toByteArray()
}
