package com.czh.rediscompose.data.storage

import com.czh.rediscompose.domain.connection.ConnectionProfile
import com.czh.rediscompose.domain.connection.SshConfig
import com.czh.rediscompose.domain.settings.AppSettings
import com.czh.rediscompose.domain.settings.ThemeMode
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StorageRepositoriesTest {
    private fun paths(): FilePaths = FilePaths(Files.createTempDirectory("redis-compose-storage-"))

    @Test
    fun `settings fall back to defaults when the file is missing`() =
        runBlocking {
            val repository = JsonSettingsRepository(paths())
            assertEquals(AppSettings(), repository.load())
        }

    @Test
    fun `settings round trip is atomic`() =
        runBlocking {
            val paths = paths()
            val repository = JsonSettingsRepository(paths)
            val settings = AppSettings(theme = ThemeMode.DARK, recentCommands = listOf("GET user:1"))
            repository.save(settings)

            assertEquals(settings, repository.load())
            assertFalse(Files.exists(paths.settings.resolveSibling("settings.json.tmp")))
        }

    @Test
    fun `corrupt settings fall back to defaults`() =
        runBlocking {
            val paths = paths()
            Files.createDirectories(paths.baseDir)
            Files.writeString(paths.settings, """{"theme": """)
            assertEquals(AppSettings(), JsonSettingsRepository(paths).load())
        }

    @Test
    fun `connections save encrypts secrets`() =
        runBlocking {
            val paths = paths()
            val cipher = AesGcmPasswordCipher(paths)
            val repository = JsonConnectionRepository(paths, cipher)
            val profile =
                ConnectionProfile(
                    id = "prod",
                    name = "Prod",
                    host = "redis.example.com",
                    password = "redis-secret",
                    ssh = SshConfig(enabled = true, host = "jump", username = "deploy", password = "ssh-secret"),
                )
            repository.save(listOf(profile))

            val raw = Files.readString(paths.connections)
            assertTrue(raw.contains("enc:v1:"))
            assertFalse(raw.contains("redis-secret"))
            assertFalse(raw.contains("ssh-secret"))
            assertFalse(Files.exists(paths.connections.resolveSibling("connections.json.tmp")))

            val loaded = repository.load().single()
            assertEquals("redis-secret", loaded.password)
            assertEquals("ssh-secret", loaded.ssh.password)
        }

    @Test
    fun `legacy plaintext connections still load`() =
        runBlocking {
            val paths = paths()
            Files.createDirectories(paths.baseDir)
            Files.writeString(
                paths.connections,
                """[{"id":"legacy","name":"Legacy","host":"127.0.0.1","password":"plain","legacyField":true}]""",
            )
            val loaded = JsonConnectionRepository(paths, AesGcmPasswordCipher(paths)).load().single()
            assertEquals("legacy", loaded.id)
            assertEquals("plain", loaded.password)
            assertEquals(6379, loaded.port)
        }

    @Test
    fun `corrupt connections file loads as empty`() =
        runBlocking {
            val paths = paths()
            Files.createDirectories(paths.baseDir)
            Files.writeString(paths.connections, "not json")
            assertTrue(JsonConnectionRepository(paths, AesGcmPasswordCipher(paths)).load().isEmpty())
        }
}
