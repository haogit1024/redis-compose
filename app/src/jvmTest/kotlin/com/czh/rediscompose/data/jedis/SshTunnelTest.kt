package com.czh.rediscompose.data.jedis

import com.czh.rediscompose.data.redis.RedisException
import com.czh.rediscompose.domain.connection.SshAuthMethod
import com.czh.rediscompose.domain.connection.SshConfig
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SshTunnelTest {
    @Test
    fun `disabled tunnel is rejected`() {
        val tunnel = SshTunnel(SshConfig(), remoteHost = "127.0.0.1", remotePort = 6379)
        assertFailsWith<RedisException> { tunnel.start() }
        tunnel.close()
    }

    @Test
    fun `key auth without a key path fails before opening a socket`() {
        val tunnel =
            SshTunnel(
                SshConfig(
                    enabled = true,
                    host = "127.0.0.1",
                    username = "deploy",
                    authMethod = SshAuthMethod.PRIVATE_KEY,
                ),
                remoteHost = "127.0.0.1",
                remotePort = 6379,
            )
        val error = assertFailsWith<RedisException> { tunnel.start() }
        assertTrue(error.message.orEmpty().contains("私钥"))
        tunnel.close()
    }

    @Test
    fun `missing key file reports a readable error and closes cleanly`() {
        val tunnel =
            SshTunnel(
                SshConfig(
                    enabled = true,
                    host = "127.0.0.1",
                    username = "deploy",
                    privateKeyPath = "/nonexistent/redis-compose/id_ed25519",
                ),
                remoteHost = "127.0.0.1",
                remotePort = 6379,
            )
        val error = assertFailsWith<RedisException> { tunnel.start() }
        assertTrue(error.message.orEmpty().contains("不存在"))
        tunnel.close()
    }
}
