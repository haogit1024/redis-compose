package com.czh.rediscompose.data.jedis

import com.czh.rediscompose.data.redis.RedisException
import com.czh.rediscompose.domain.connection.TlsConfig
import com.czh.rediscompose.domain.connection.TlsVerifyMode
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TlsContextFactoryTest {
    @Test
    fun `strict system trust does not build a custom socket factory`() {
        assertNull(TlsContextFactory.socketFactory(TlsConfig(enabled = true)))
    }

    @Test
    fun `non strict verification builds a trust all factory`() {
        assertNotNull(
            TlsContextFactory.socketFactory(
                TlsConfig(enabled = true, verifyMode = TlsVerifyMode.ALLOW_SELF_SIGNED),
            ),
        )
        assertNotNull(
            TlsContextFactory.socketFactory(
                TlsConfig(enabled = true, verifyMode = TlsVerifyMode.NONE),
            ),
        )
    }

    @Test
    fun `client certificate requires both files`() {
        val error =
            assertFailsWith<RedisException> {
                TlsContextFactory.socketFactory(
                    TlsConfig(enabled = true, clientCertPath = "/nonexistent/client.pem"),
                )
            }
        assertTrue(error.message.orEmpty().contains("必须同时提供"))
    }

    @Test
    fun `missing ca file fails with a readable error`() {
        val error =
            assertFailsWith<RedisException> {
                TlsContextFactory.socketFactory(
                    TlsConfig(enabled = true, caPath = "/nonexistent/redis-compose/ca.pem"),
                )
            }
        assertTrue(error.message.orEmpty().contains("CA"))
    }

    @Test
    fun `missing client certificate file fails without downgrading`() {
        val error =
            assertFailsWith<RedisException> {
                TlsContextFactory.socketFactory(
                    TlsConfig(
                        enabled = true,
                        clientCertPath = "/nonexistent/client.pem",
                        clientKeyPath = "/nonexistent/client-key.pem",
                    ),
                )
            }
        assertTrue(error.message.orEmpty().contains("客户端证书"))
    }
}
