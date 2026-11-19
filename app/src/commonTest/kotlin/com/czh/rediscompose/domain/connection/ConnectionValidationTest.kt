package com.czh.rediscompose.domain.connection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConnectionValidationTest {
    private fun profile(): ConnectionProfile = ConnectionProfile(id = "local", name = "Local", host = "127.0.0.1")

    @Test
    fun `default profile is valid`() {
        assertTrue(validateConnection(profile()).isValid)
    }

    @Test
    fun `name host port and database are checked`() {
        val errors =
            validateConnection(
                profile().copy(name = " ", host = "", port = 0, defaultDatabase = 300),
            ).errors
        assertEquals("请输入连接名称", errors[ConnectionField.NAME])
        assertEquals("请输入主机地址", errors[ConnectionField.HOST])
        assertTrue(errors.containsKey(ConnectionField.PORT))
        assertTrue(errors.containsKey(ConnectionField.DATABASE))
    }

    @Test
    fun `valid port boundaries are accepted`() {
        assertTrue(validateConnection(profile().copy(port = 1)).isValid)
        assertTrue(validateConnection(profile().copy(port = 65535)).isValid)
        assertFalse(validateConnection(profile().copy(port = 65536)).isValid)
    }

    @Test
    fun `tls paths must be absolute or home relative`() {
        val relative =
            profile().copy(
                tls = TlsConfig(enabled = true, caPath = "certs/ca.pem"),
            )
        val errors = validateConnection(relative).errors
        assertEquals("请填写绝对路径或以 ~ 开头", errors[ConnectionField.TLS_CA])

        assertTrue(
            validateConnection(
                profile().copy(tls = TlsConfig(enabled = true, caPath = "/etc/ca.pem")),
            ).isValid,
        )
        assertTrue(
            validateConnection(
                profile().copy(tls = TlsConfig(enabled = true, caPath = "~/ca.pem")),
            ).isValid,
        )
    }

    @Test
    fun `tls paths are ignored while tls is disabled`() {
        val profile = profile().copy(tls = TlsConfig(enabled = false, caPath = "relative.pem"))
        assertNull(validateConnection(profile).errors[ConnectionField.TLS_CA])
    }

    @Test
    fun `ssh requires host user and key for key auth`() {
        val errors =
            validateConnection(
                profile().copy(ssh = SshConfig(enabled = true)),
            ).errors
        assertTrue(errors.containsKey(ConnectionField.SSH_HOST))
        assertTrue(errors.containsKey(ConnectionField.SSH_USER))
        assertTrue(errors.containsKey(ConnectionField.SSH_KEY))
    }

    @Test
    fun `ssh password auth requires a password and no private key`() {
        val missingPassword =
            profile().copy(
                ssh =
                    SshConfig(
                        enabled = true,
                        host = "jump.example.com",
                        username = "deploy",
                        authMethod = SshAuthMethod.PASSWORD,
                    ),
            )
        assertTrue(validateConnection(missingPassword).errors.containsKey(ConnectionField.SSH_PASSWORD))

        val withPassword =
            missingPassword.copy(ssh = missingPassword.ssh.copy(password = "secret"))
        assertTrue(validateConnection(withPassword).isValid)
    }

    @Test
    fun `tls client certificate and key must be provided together`() {
        val onlyCertificate =
            profile().copy(
                tls = TlsConfig(enabled = true, clientCertPath = "/etc/client.pem"),
            )
        val errors = validateConnection(onlyCertificate).errors
        assertEquals("请选择客户端私钥", errors[ConnectionField.TLS_KEY])

        val onlyKey =
            profile().copy(
                tls = TlsConfig(enabled = true, clientKeyPath = "/etc/client-key.pem"),
            )
        assertEquals("请选择客户端证书", validateConnection(onlyKey).errors[ConnectionField.TLS_CERT])
    }

    @Test
    fun `connect and read timeouts must be positive`() {
        val errors =
            validateConnection(
                profile().copy(connectTimeoutMillis = 0, readTimeoutMillis = -1),
            ).errors
        assertTrue(errors.containsKey(ConnectionField.CONNECT_TIMEOUT))
        assertTrue(errors.containsKey(ConnectionField.READ_TIMEOUT))
    }

    @Test
    fun `ssh port is validated`() {
        val errors =
            validateConnection(
                profile().copy(
                    ssh =
                        SshConfig(
                            enabled = true,
                            host = "jump",
                            port = 0,
                            username = "deploy",
                            privateKeyPath = "~/.ssh/id_ed25519",
                        ),
                ),
            ).errors
        assertTrue(errors.containsKey(ConnectionField.SSH_PORT))
    }
}
