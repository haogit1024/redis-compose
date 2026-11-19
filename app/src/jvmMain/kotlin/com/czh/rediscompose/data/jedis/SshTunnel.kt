package com.czh.rediscompose.data.jedis

import com.czh.rediscompose.data.redis.RedisException
import com.czh.rediscompose.data.storage.expandUserPath
import com.czh.rediscompose.domain.connection.SshAuthMethod
import com.czh.rediscompose.domain.connection.SshConfig
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import java.nio.file.Files
import java.util.Properties

/**
 * Local port forward for a Redis server reached through a jump host.
 *
 * The tunnel is created lazily by [JedisRedisGateway] and always closed with the gateway.
 * Host-key checking is disabled in v1 (`StrictHostKeyChecking=no`); the connection sheet
 * shows that warning explicitly.
 */
internal class SshTunnel(
    private val config: SshConfig,
    private val remoteHost: String,
    private val remotePort: Int,
) {
    private var session: Session? = null
    private var localPortValue: Int? = null

    val localHost: String get() = "127.0.0.1"

    val localPort: Int
        get() = localPortValue ?: throw RedisException("SSH 隧道尚未建立")

    fun start(): Int {
        synchronized(this) {
            localPortValue?.let { return it }
            if (!config.enabled) throw RedisException("SSH 隧道未启用")
            if (config.host.isBlank() || config.username.isBlank()) {
                throw RedisException("SSH 隧道缺少 Host 或用户名")
            }
            val jsch = JSch()
            try {
                when (config.authMethod) {
                    SshAuthMethod.PRIVATE_KEY -> configurePrivateKey(jsch)
                    SshAuthMethod.PASSWORD -> Unit
                }
                val newSession = jsch.getSession(config.username, config.host, config.port)
                if (config.authMethod == SshAuthMethod.PASSWORD) {
                    if (config.password.isEmpty()) throw RedisException("SSH 密码为空")
                    newSession.setPassword(config.password.toByteArray(Charsets.UTF_8))
                }
                val options =
                    Properties().apply {
                        put("StrictHostKeyChecking", "no")
                        put(
                            "PreferredAuthentications",
                            if (config.authMethod == SshAuthMethod.PASSWORD) {
                                "password,keyboard-interactive,publickey"
                            } else {
                                "publickey,password,keyboard-interactive"
                            },
                        )
                    }
                newSession.setConfig(options)
                newSession.connect(CONNECT_TIMEOUT_MILLIS)
                val localPort = newSession.setPortForwardingL(0, remoteHost, remotePort)
                session = newSession
                localPortValue = localPort
                return localPort
            } catch (error: RedisException) {
                close()
                throw error
            } catch (error: JSchException) {
                close()
                throw RedisException("SSH 隧道连接失败：${error.firstLine()}", error)
            } catch (error: Exception) {
                close()
                throw RedisException("SSH 隧道连接失败：${error.firstLine()}", error)
            }
        }
    }

    fun close() {
        synchronized(this) {
            val active = session
            val port = localPortValue
            session = null
            localPortValue = null
            if (active != null) {
                if (port != null) runCatching { active.delPortForwardingL(port) }
                runCatching { active.disconnect() }
            }
        }
    }

    private fun configurePrivateKey(jsch: JSch) {
        if (config.privateKeyPath.isBlank()) throw RedisException("SSH 私钥路径为空")
        val path = expandUserPath(config.privateKeyPath)
        if (!Files.isRegularFile(path)) {
            throw RedisException("SSH 私钥文件不存在：${config.privateKeyPath}")
        }
        val keyPath = path.toString()
        if (config.password.isNotEmpty()) {
            jsch.addIdentity(keyPath, config.password.toByteArray(Charsets.UTF_8))
        } else {
            jsch.addIdentity(keyPath)
        }
    }

    private fun Throwable.firstLine(): String =
        message?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() }
            ?: this::class.simpleName
            ?: "未知错误"

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 10_000
    }
}
