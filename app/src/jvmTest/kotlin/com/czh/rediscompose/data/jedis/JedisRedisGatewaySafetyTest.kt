package com.czh.rediscompose.data.jedis

import com.czh.rediscompose.data.redis.RedisException
import com.czh.rediscompose.domain.connection.ConnectionProfile
import com.czh.rediscompose.domain.connection.SshAuthMethod
import com.czh.rediscompose.domain.connection.SshConfig
import com.czh.rediscompose.domain.connection.TlsConfig
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class JedisRedisGatewaySafetyTest {
    @Test
    fun `ssh profiles fail instead of connecting directly`() =
        runBlocking {
            val gateway =
                JedisRedisGateway(
                    ConnectionProfile(
                        id = "ssh",
                        name = "SSH",
                        host = "127.0.0.1",
                        ssh =
                            SshConfig(
                                enabled = true,
                                host = "127.0.0.1",
                                port = 1,
                                username = "deploy",
                                authMethod = SshAuthMethod.PASSWORD,
                                password = "secret",
                            ),
                    ),
                )
            val error = assertFailsWith<RedisException> { gateway.ping() }
            assertTrue(error.message.orEmpty().contains("SSH"))
            gateway.close()
        }

    @Test
    fun `client certificates fail fast instead of downgrading`() =
        runBlocking {
            val gateway =
                JedisRedisGateway(
                    ConnectionProfile(
                        id = "tls",
                        name = "TLS",
                        host = "127.0.0.1",
                        tls =
                            TlsConfig(
                                enabled = true,
                                clientCertPath = "/nonexistent/redis-compose/client.pem",
                                clientKeyPath = "/nonexistent/redis-compose/client-key.pem",
                            ),
                    ),
                )
            val error = assertFailsWith<RedisException> { gateway.ping() }
            assertTrue(error.message.orEmpty().contains("证书"))
            gateway.close()
        }
}
