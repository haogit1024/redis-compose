package com.czh.rediscompose.data.jedis

import com.czh.rediscompose.data.redis.ClientListParser
import com.czh.rediscompose.data.redis.HashFieldBytes
import com.czh.rediscompose.data.redis.InfoParser
import com.czh.rediscompose.data.redis.RedisException
import com.czh.rediscompose.data.redis.RedisGateway
import com.czh.rediscompose.data.redis.RedisReplyParser
import com.czh.rediscompose.data.redis.SlowLogParser
import com.czh.rediscompose.data.redis.SortedSetEntryBytes
import com.czh.rediscompose.data.redis.StreamEntryBytes
import com.czh.rediscompose.data.redis.StreamParser
import com.czh.rediscompose.data.redis.ValueCodec
import com.czh.rediscompose.data.storage.expandUserPath
import com.czh.rediscompose.domain.command.ClientConnection
import com.czh.rediscompose.domain.command.CommandStat
import com.czh.rediscompose.domain.command.RedisReply
import com.czh.rediscompose.domain.command.SlowLogEntry
import com.czh.rediscompose.domain.connection.ConnectionProfile
import com.czh.rediscompose.domain.connection.TlsVerifyMode
import com.czh.rediscompose.domain.key.KeyType
import com.czh.rediscompose.domain.key.ScanPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisClientConfig
import redis.clients.jedis.commands.ProtocolCommand
import redis.clients.jedis.exceptions.JedisConnectionException
import redis.clients.jedis.exceptions.JedisDataException
import redis.clients.jedis.exceptions.JedisException
import redis.clients.jedis.params.ScanParams
import java.nio.file.Files
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Jedis-backed gateway.
 *
 * A single [Jedis] connection is created lazily and all calls are serialized through a
 * mutex on [Dispatchers.IO]. SSH is intentionally not tunneled in Phase 2 and fails with
 * a clear message instead of connecting directly.
 */
class JedisRedisGateway(
    private val profile: ConnectionProfile,
) : RedisGateway {
    private val mutex = Mutex()

    @Volatile
    private var jedis: Jedis? = null

    override suspend fun ping(): String = call { it.ping() }

    override suspend fun info(section: String?): String = call { client -> if (section == null) client.info() else client.info(section) }

    override suspend fun selectDatabase(index: Int) {
        call { it.select(index) }
    }

    override suspend fun databaseSize(): Long = call { it.dbSize() }

    override suspend fun scan(
        cursor: Long,
        pattern: String?,
        count: Int,
    ): ScanPage {
        val params = ScanParams().count(count.coerceAtLeast(1))
        if (!pattern.isNullOrBlank()) params.match(pattern)
        val result = call { it.scan(java.lang.Long.toUnsignedString(cursor), params) }
        return ScanPage(
            cursor = java.lang.Long.parseUnsignedLong(result.cursor),
            keys = result.result,
        )
    }

    override suspend fun keyType(key: String): KeyType {
        val type = call { it.type(key) }.uppercase()
        return KeyType.entries.firstOrNull { it.name == type } ?: KeyType.NONE
    }

    override suspend fun keyTtl(key: String): Long = call { it.ttl(key) }

    override suspend fun keyMemoryUsage(key: String): Long? = callOrNull { it.memoryUsage(key.toByteArray()) }

    override suspend fun keyEncoding(key: String): String? {
        val encoding = callOrNull { it.objectEncoding(key.toByteArray()) } ?: return null
        return ValueCodec.decode(encoding) ?: ValueCodec.hex(encoding)
    }

    override suspend fun getString(key: String): ByteArray? = call { it.get(key.toByteArray()) }

    override suspend fun setString(
        key: String,
        value: ByteArray,
    ) {
        call { it.set(key.toByteArray(), value) }
    }

    override suspend fun getHash(key: String): List<HashFieldBytes> =
        call { it.hgetAll(key.toByteArray()) }
            .entries
            .map { (field, value) ->
                HashFieldBytes(field = ValueCodec.decode(field) ?: ValueCodec.hex(field), value = value)
            }
            .sortedBy { it.field.lowercase() }

    override suspend fun hashSet(
        key: String,
        field: String,
        value: ByteArray,
    ) {
        call { it.hset(key.toByteArray(), field.toByteArray(Charsets.UTF_8), value) }
    }

    override suspend fun hashDelete(
        key: String,
        field: String,
    ) {
        call { it.hdel(key.toByteArray(), field.toByteArray(Charsets.UTF_8)) }
    }

    override suspend fun getList(key: String): List<ByteArray> = call { it.lrange(key.toByteArray(), 0, -1) }

    override suspend fun listPush(
        key: String,
        value: ByteArray,
        head: Boolean,
    ) {
        call { client ->
            if (head) {
                client.lpush(key.toByteArray(), value)
            } else {
                client.rpush(key.toByteArray(), value)
            }
        }
    }

    override suspend fun listSet(
        key: String,
        index: Long,
        value: ByteArray,
    ) {
        call { it.lset(key.toByteArray(), index, value) }
    }

    override suspend fun listRemove(
        key: String,
        value: ByteArray,
    ) {
        call { it.lrem(key.toByteArray(), 1L, value) }
    }

    override suspend fun getSet(key: String): List<ByteArray> =
        call { it.smembers(key.toByteArray()).toList() }
            .sortedBy { member -> ValueCodec.decode(member) ?: ValueCodec.hex(member) }

    override suspend fun setAdd(
        key: String,
        value: ByteArray,
    ) {
        call { it.sadd(key.toByteArray(), value) }
    }

    override suspend fun setRemove(
        key: String,
        value: ByteArray,
    ) {
        call { it.srem(key.toByteArray(), value) }
    }

    override suspend fun getSortedSet(key: String): List<SortedSetEntryBytes> =
        call { it.zrangeWithScores(key.toByteArray(), 0, -1) }
            .map { SortedSetEntryBytes(member = it.binaryElement, score = it.score) }

    override suspend fun sortedSetAdd(
        key: String,
        member: ByteArray,
        score: Double,
    ) {
        call { it.zadd(key.toByteArray(), score, member) }
    }

    override suspend fun sortedSetRemove(
        key: String,
        member: ByteArray,
    ) {
        call { it.zrem(key.toByteArray(), member) }
    }

    override suspend fun getStream(
        key: String,
        count: Int,
    ): List<StreamEntryBytes> {
        val args =
            buildList {
                add("XRANGE")
                add(key)
                add("-")
                add("+")
                if (count > 0) {
                    add("COUNT")
                    add(count.toString())
                }
            }
        return StreamParser.parse(execute(args))
    }

    override suspend fun getStreamGroupCount(key: String): Int? =
        when (val reply = execute(listOf("XINFO", "GROUPS", key))) {
            is RedisReply.Array -> reply.items.size
            else -> null
        }

    override suspend fun deleteKeys(keys: List<String>) {
        if (keys.isEmpty()) return
        call { it.del(*keys.toTypedArray()) }
    }

    override suspend fun renameKey(
        oldKey: String,
        newKey: String,
    ) {
        call { it.rename(oldKey, newKey) }
    }

    override suspend fun expire(
        key: String,
        seconds: Long,
    ) {
        call { it.expire(key, seconds) }
    }

    override suspend fun persist(key: String) {
        call { it.persist(key) }
    }

    override suspend fun slowLog(count: Int): List<SlowLogEntry> {
        val reply = execute(listOf("SLOWLOG", "GET", count.coerceAtLeast(1).toString()))
        if (reply is RedisReply.Error) throw RedisException(reply.message)
        return SlowLogParser.parse(reply)
    }

    override suspend fun clients(): List<ClientConnection> {
        val reply = execute(listOf("CLIENT", "LIST"))
        if (reply is RedisReply.Error) throw RedisException(reply.message)
        val text =
            (reply as? RedisReply.Bulk)?.let { ValueCodec.decode(it.value) }
                ?: throw RedisException("CLIENT LIST 返回了意外的格式")
        return ClientListParser.parse(text)
    }

    override suspend fun commandStats(): List<CommandStat> = InfoParser.commandStats(info("commandstats"))

    override suspend fun execute(args: List<String>): RedisReply {
        if (args.isEmpty()) return RedisReply.Error("命令不能为空")
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                val client = ensureClient()
                try {
                    val command = ProtocolCommand { args.first().toByteArray(Charsets.UTF_8) }
                    val raw = client.sendCommand(command, *args.drop(1).toTypedArray())
                    RedisReplyParser.fromRaw(raw)
                } catch (error: JedisDataException) {
                    RedisReply.Error(error.firstLine())
                } catch (error: JedisConnectionException) {
                    closeInternal()
                    throw RedisException("连接失败：${error.firstLine()}", error)
                } catch (error: JedisException) {
                    throw RedisException(error.firstLine(), error)
                }
            }
        }
    }

    override fun close() {
        closeInternal()
    }

    private suspend fun <T> call(block: (Jedis) -> T): T =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val client = ensureClient()
                try {
                    block(client)
                } catch (error: JedisDataException) {
                    throw RedisException(error.firstLine(), error)
                } catch (error: JedisConnectionException) {
                    closeInternal()
                    throw RedisException("连接失败：${error.firstLine()}", error)
                } catch (error: JedisException) {
                    throw RedisException(error.firstLine(), error)
                }
            }
        }

    private suspend fun <T> callOrNull(block: (Jedis) -> T?): T? =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val client = ensureClient()
                try {
                    block(client)
                } catch (_: JedisDataException) {
                    null
                } catch (error: JedisConnectionException) {
                    closeInternal()
                    throw RedisException("连接失败：${error.firstLine()}", error)
                } catch (error: JedisException) {
                    throw RedisException(error.firstLine(), error)
                }
            }
        }

    private fun ensureClient(): Jedis {
        if (profile.ssh.enabled) {
            throw RedisException("SSH 隧道尚未启用，将在 Phase 8 支持")
        }
        jedis?.let { existing ->
            if (existing.isConnected) return existing
            runCatching { existing.close() }
            jedis = null
        }
        val client = Jedis(profile.host, profile.port, buildClientConfig())
        return try {
            client.connect()
            if (profile.defaultDatabase != 0) client.select(profile.defaultDatabase)
            jedis = client
            client
        } catch (error: Exception) {
            runCatching { client.close() }
            throw if (error is RedisException) error else RedisException("连接失败：${error.firstLine()}", error)
        }
    }

    private fun buildClientConfig(): JedisClientConfig {
        if (profile.tls.enabled &&
            (profile.tls.clientCertPath.isNotBlank() || profile.tls.clientKeyPath.isNotBlank())
        ) {
            throw RedisException("客户端证书认证将在 Phase 8 支持")
        }
        val builder =
            DefaultJedisClientConfig.builder()
                .connectionTimeoutMillis(profile.connectTimeoutMillis)
                .socketTimeoutMillis(profile.readTimeoutMillis)
                .database(profile.defaultDatabase)
        if (profile.username.isNotBlank()) builder.user(profile.username)
        if (profile.password.isNotEmpty()) builder.password(profile.password)
        if (profile.tls.enabled) {
            builder.ssl(true)
            val verifyMode = profile.tls.verifyMode
            val socketFactory =
                when {
                    profile.tls.caPath.isNotBlank() -> sslContextForCa(profile.tls.caPath).socketFactory
                    verifyMode != TlsVerifyMode.STRICT -> trustAllSslContext().socketFactory
                    else -> null
                }
            if (socketFactory != null) builder.sslSocketFactory(socketFactory)
            if (verifyMode == TlsVerifyMode.NONE) {
                builder.hostnameVerifier(HostnameVerifier { _, _ -> true })
            }
        }
        return builder.build()
    }

    private fun sslContextForCa(caPath: String): SSLContext {
        val context =
            runCatching {
                val certificates =
                    Files.newInputStream(expandUserPath(caPath)).use { stream ->
                        CertificateFactory.getInstance("X.509").generateCertificates(stream)
                    }
                if (certificates.isEmpty()) error("empty certificate file")
                val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply { load(null, null) }
                certificates.forEachIndexed { index, certificate ->
                    keyStore.setCertificateEntry("ca-$index", certificate)
                }
                val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                factory.init(keyStore)
                SSLContext.getInstance("TLS").apply {
                    init(null, factory.trustManagers, SecureRandom())
                }
            }.getOrElse { error ->
                throw RedisException("无法读取自定义 CA 证书：${error.firstLine()}", error)
            }
        return context
    }

    private fun trustAllSslContext(): SSLContext {
        val trustManager =
            object : X509TrustManager {
                override fun checkClientTrusted(
                    chain: Array<out java.security.cert.X509Certificate>?,
                    authType: String?,
                ) = Unit

                override fun checkServerTrusted(
                    chain: Array<out java.security.cert.X509Certificate>?,
                    authType: String?,
                ) = Unit

                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = emptyArray()
            }
        return SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
        }
    }

    private fun closeInternal() {
        synchronized(this) {
            runCatching { jedis?.close() }
            jedis = null
        }
    }

    private fun Throwable.firstLine(): String =
        message?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() }
            ?: this::class.simpleName
            ?: "未知错误"
}
