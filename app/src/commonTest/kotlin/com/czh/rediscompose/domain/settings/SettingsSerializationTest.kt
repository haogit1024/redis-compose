package com.czh.rediscompose.domain.settings

import com.czh.rediscompose.domain.connection.ConnectionEnvironment
import com.czh.rediscompose.domain.connection.ConnectionProfile
import com.czh.rediscompose.domain.connection.SshAuthMethod
import com.czh.rediscompose.domain.connection.SshConfig
import com.czh.rediscompose.domain.connection.TlsConfig
import com.czh.rediscompose.domain.connection.TlsVerifyMode
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `defaults match the design defaults`() {
        val settings = AppSettings()
        assertEquals(ThemeMode.SYSTEM, settings.theme)
        assertEquals(Accent.IRIS, settings.accent)
        assertEquals(UiDensity.COMFORTABLE, settings.density)
        assertEquals(KeyView.TREE, settings.keyView)
        assertEquals(ValueFormat.AUTO, settings.defaultValueFormat)
        assertEquals(1_000, settings.scanBatchSize)
        assertTrue(settings.encryptPasswords)
    }

    @Test
    fun `settings round trip keeps values and enums`() {
        val settings =
            AppSettings(
                theme = ThemeMode.DARK,
                accent = Accent.MINT,
                density = UiDensity.COMPACT,
                keyView = KeyView.FLAT,
                defaultValueFormat = ValueFormat.HEX,
                recentCommands = listOf("GET user:1", "INFO"),
            )
        val decoded = json.decodeFromString<AppSettings>(json.encodeToString(settings))
        assertEquals(settings, decoded)
    }

    @Test
    fun `unknown fields are ignored`() {
        val decoded =
            json.decodeFromString<AppSettings>(
                """{"theme":"LIGHT","futureSetting":{"nested":true}}""",
            )
        assertEquals(ThemeMode.LIGHT, decoded.theme)
    }

    @Test
    fun `connection profile round trip keeps nested config`() {
        val profile =
            ConnectionProfile(
                id = "prod",
                name = "Prod Cache",
                environment = ConnectionEnvironment.PRODUCTION,
                host = "redis.example.com",
                port = 6380,
                username = "app",
                password = "secret",
                defaultDatabase = 3,
                group = "Production",
                tls =
                    TlsConfig(
                        enabled = true,
                        caPath = "~/certs/ca.pem",
                        verifyMode = TlsVerifyMode.ALLOW_SELF_SIGNED,
                    ),
                ssh =
                    SshConfig(
                        enabled = true,
                        host = "jump.example.com",
                        username = "deploy",
                        authMethod = SshAuthMethod.PASSWORD,
                        password = "jump-secret",
                    ),
            )
        val decoded = json.decodeFromString<ConnectionProfile>(json.encodeToString(profile))
        assertEquals(profile, decoded)
    }

    @Test
    fun `old connection json without new fields uses defaults`() {
        val decoded =
            json.decodeFromString<ConnectionProfile>(
                """{"id":"old","name":"Old","host":"127.0.0.1","unknown":"ignored"}""",
            )
        assertEquals(6379, decoded.port)
        assertEquals(ConnectionEnvironment.LOCAL, decoded.environment)
        assertEquals(":", decoded.keySeparator)
        assertEquals(TlsVerifyMode.STRICT, decoded.tls.verifyMode)
        assertEquals(SshAuthMethod.PRIVATE_KEY, decoded.ssh.authMethod)
    }
}
