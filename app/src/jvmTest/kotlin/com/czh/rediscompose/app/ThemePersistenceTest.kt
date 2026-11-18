package com.czh.rediscompose.app

import com.czh.rediscompose.data.redis.RedisGatewayFactory
import com.czh.rediscompose.data.storage.FilePaths
import com.czh.rediscompose.data.storage.JsonSettingsRepository
import com.czh.rediscompose.domain.connection.ConnectionProfile
import com.czh.rediscompose.domain.connection.ConnectionRepository
import com.czh.rediscompose.domain.repository.PasswordCipher
import com.czh.rediscompose.domain.settings.AppSettings
import com.czh.rediscompose.domain.settings.SettingsRepository
import com.czh.rediscompose.domain.settings.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemePersistenceTest {
    @Test
    fun `theme updates survive a repository reload`() =
        runBlocking {
            val paths = FilePaths(Files.createTempDirectory("redis-compose-theme-"))
            val settingsRepository = JsonSettingsRepository(paths)
            val container = TestContainer(settingsRepository)
            val state =
                AppState(
                    container = container,
                    scope = CoroutineScope(Dispatchers.Unconfined),
                    initialSettings = settingsRepository.load(),
                    initialConnections = emptyList(),
                )

            state.updateTheme(ThemeMode.DARK)

            assertEquals(ThemeMode.DARK, state.settings.theme)
            withTimeout(2_000) {
                while (JsonSettingsRepository(paths).load().theme != ThemeMode.DARK) {
                    delay(20)
                }
            }
            assertEquals(ThemeMode.DARK, JsonSettingsRepository(paths).load().theme)
            state.close()
        }

    private class TestContainer(
        override val settingsRepository: SettingsRepository,
    ) : AppContainer {
        override val connectionRepository: ConnectionRepository =
            object : ConnectionRepository {
                override suspend fun load(): List<ConnectionProfile> = emptyList()

                override suspend fun save(connections: List<ConnectionProfile>) = Unit
            }
        override val gatewayFactory: RedisGatewayFactory =
            RedisGatewayFactory {
                error("Not used in this test")
            }
        override val passwordCipher: PasswordCipher =
            object : PasswordCipher {
                override fun encrypt(plain: String): String = plain

                override fun decrypt(stored: String): String = stored
            }
        override val initialSettings: AppSettings = AppSettings()
        override val initialConnections: List<ConnectionProfile> = emptyList()
    }
}
