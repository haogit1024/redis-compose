package com.czh.rediscompose.feature.connections

import com.czh.rediscompose.domain.connection.ConnectionEnvironment
import com.czh.rediscompose.domain.connection.ConnectionField
import com.czh.rediscompose.domain.connection.ConnectionProfile
import com.czh.rediscompose.domain.connection.SshAuthMethod
import com.czh.rediscompose.domain.connection.TlsVerifyMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConnectionDraftTest {
    private fun profile() =
        ConnectionProfile(
            id = "prod",
            name = "Prod Cache",
            environment = ConnectionEnvironment.PRODUCTION,
            host = "redis-01.acme.io",
            port = 6380,
            username = "app",
            password = "secret",
            defaultDatabase = 2,
            keySeparator = ":",
            group = "Production",
            connectTimeoutMillis = 6_000,
            readTimeoutMillis = 7_000,
            lastOpenedAtEpochMillis = 123_000L,
            createdAtEpochMillis = 1_000L,
        )

    @Test
    fun `new draft reports required fields`() {
        val errors = ConnectionDraft.new().copy(host = "").validate(nowEpochMillis = 10L)

        assertTrue(errors.containsKey(ConnectionField.NAME))
        assertTrue(errors.containsKey(ConnectionField.HOST))
    }

    @Test
    fun `draft converts to a profile and trims display fields`() {
        val draft =
            ConnectionDraft.new().copy(
                name = "  Local  ",
                host = "  127.0.0.1 ",
                port = "6380",
                password = "secret",
                defaultDatabase = "3",
                group = " Dev ",
                color = com.czh.rediscompose.domain.connection.ConnectionColor.MINT,
            )

        val profile = draft.toProfile(nowEpochMillis = 5_000L, id = "new-id")

        assertEquals("new-id", profile.id)
        assertEquals("Local", profile.name)
        assertEquals("127.0.0.1", profile.host)
        assertEquals(6380, profile.port)
        assertEquals(3, profile.defaultDatabase)
        assertEquals("Dev", profile.group)
        assertEquals(5_000L, profile.createdAtEpochMillis)
        assertEquals(5_000L, profile.updatedAtEpochMillis)
    }

    @Test
    fun `invalid numbers produce field errors`() {
        val errors =
            ConnectionDraft.new()
                .copy(port = "abc", defaultDatabase = "x", connectTimeout = "no", readTimeout = "")
                .validate(nowEpochMillis = 1L)

        assertTrue(errors.containsKey(ConnectionField.PORT))
        assertTrue(errors.containsKey(ConnectionField.DATABASE))
        assertTrue(errors.containsKey(ConnectionField.CONNECT_TIMEOUT))
        assertTrue(errors.containsKey(ConnectionField.READ_TIMEOUT))
    }

    @Test
    fun `edit draft preserves id createdAt and recent timestamp`() {
        val draft = ConnectionDraft.fromProfile(profile(), ConnectionFormMode.EDIT)

        val updated = draft.copy(name = "Prod Cache 2").toProfile(nowEpochMillis = 9_000L)

        assertEquals("prod", updated.id)
        assertEquals(1_000L, updated.createdAtEpochMillis)
        assertEquals(9_000L, updated.updatedAtEpochMillis)
        assertEquals(123_000L, updated.lastOpenedAtEpochMillis)
        assertEquals("Prod Cache 2", updated.name)
    }

    @Test
    fun `duplicate mode starts from a new id with copied settings`() {
        val source = profile().copy(id = "copy-id", name = "Prod Cache 副本", lastOpenedAtEpochMillis = null)
        val draft = ConnectionDraft.fromProfile(source, ConnectionFormMode.DUPLICATE)

        assertEquals(ConnectionFormMode.DUPLICATE, draft.mode)
        assertEquals("copy-id", draft.id)
        assertNull(draft.lastOpenedAtEpochMillis)
        assertTrue(draft.title.contains("复制"))
        assertTrue(draft.validate(nowEpochMillis = 2_000L).isEmpty())
    }

    @Test
    fun `draft carries tls and ssh options`() {
        val draft =
            ConnectionDraft.new().copy(
                name = "Secure",
                tlsEnabled = true,
                tlsCaPath = "/etc/ca.pem",
                tlsClientCertPath = "/etc/client.pem",
                tlsClientKeyPath = "/etc/client-key.pem",
                tlsVerifyMode = TlsVerifyMode.ALLOW_SELF_SIGNED,
                sshEnabled = true,
                sshHost = "jump",
                sshUsername = "deploy",
                sshAuthMethod = SshAuthMethod.PASSWORD,
                sshPassword = "secret",
            )

        val profile = draft.toProfile(nowEpochMillis = 3_000L, id = "secure")

        assertTrue(profile.tls.enabled)
        assertEquals(TlsVerifyMode.ALLOW_SELF_SIGNED, profile.tls.verifyMode)
        assertTrue(profile.ssh.enabled)
        assertEquals(SshAuthMethod.PASSWORD, profile.ssh.authMethod)
        assertEquals("secret", profile.ssh.password)
        assertTrue(draft.validate(nowEpochMillis = 3_000L).isEmpty())
    }

    @Test
    fun `new draft uses the global key separator`() {
        assertEquals("|", ConnectionDraft.new(keySeparator = "|").keySeparator)
        assertEquals(":", ConnectionDraft.new().keySeparator)
    }

    @Test
    fun `new draft generates a stable fallback id when none is supplied`() {
        val first = ConnectionDraft.new().toProfile(nowEpochMillis = 42L)
        val second = ConnectionDraft.new().toProfile(nowEpochMillis = 43L)

        assertEquals("conn-42", first.id)
        assertEquals("conn-43", second.id)
        assertFalse(first.id == second.id)
    }
}
