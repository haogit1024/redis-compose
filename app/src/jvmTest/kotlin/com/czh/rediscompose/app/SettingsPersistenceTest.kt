package com.czh.rediscompose.app

import com.czh.rediscompose.data.redis.RedisGatewayFactory
import com.czh.rediscompose.data.storage.FilePaths
import com.czh.rediscompose.data.storage.JsonSettingsRepository
import com.czh.rediscompose.design.component.ToastKind
import com.czh.rediscompose.domain.connection.ConnectionProfile
import com.czh.rediscompose.domain.connection.ConnectionRepository
import com.czh.rediscompose.domain.repository.PasswordCipher
import com.czh.rediscompose.domain.settings.Accent
import com.czh.rediscompose.domain.settings.AppSettings
import com.czh.rediscompose.domain.settings.KeyView
import com.czh.rediscompose.domain.settings.SettingsRepository
import com.czh.rediscompose.domain.settings.ThemeMode
import com.czh.rediscompose.domain.settings.UiDensity
import com.czh.rediscompose.domain.settings.ValueFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsPersistenceTest {
    @Test
    fun `phase 9 settings survive a debounced reload`() =
        runBlocking {
            val paths = FilePaths(Files.createTempDirectory("redis-compose-settings-"))
            val repository = JsonSettingsRepository(paths)
            val expected =
                AppSettings(
                    theme = ThemeMode.LIGHT,
                    accent = Accent.MINT,
                    density = UiDensity.COMPACT,
                    editorFontSize = 16,
                    defaultValueFormat = ValueFormat.JSON,
                    wordWrap = false,
                    showLineNumbers = false,
                    scanBatchSize = 2_500,
                    truncateValueKb = 32,
                    keyView = KeyView.FLAT,
                    defaultKeySeparator = "|",
                    showKeyTtl = false,
                    confirmDeleteKeys = false,
                    confirmDangerousCommands = false,
                    maskSensitiveValues = true,
                    globalReadOnly = true,
                    encryptPasswords = false,
                    autoRefreshSeconds = 60,
                )
            val state =
                AppState(
                    container = TestContainer(repository),
                    scope = CoroutineScope(Dispatchers.Unconfined),
                    initialSettings = AppSettings(),
                    initialConnections = emptyList(),
                )

            state.updateSettings(expected)

            assertEquals(expected, state.settings)
            withTimeout(3_000) {
                while (JsonSettingsRepository(paths).load() != expected) {
                    delay(20)
                }
            }
            assertEquals(SettingsSaveStatus.SAVED, state.settingsSaveStatus)
            state.close()
        }

    @Test
    fun `debounce keeps only the latest settings snapshot`() =
        runBlocking {
            val paths = FilePaths(Files.createTempDirectory("redis-compose-settings-debounce-"))
            val repository = JsonSettingsRepository(paths)
            val state =
                AppState(
                    container = TestContainer(repository),
                    scope = CoroutineScope(Dispatchers.Unconfined),
                    initialSettings = AppSettings(),
                    initialConnections = emptyList(),
                )

            state.updateSettings(AppSettings(theme = ThemeMode.DARK, editorFontSize = 12))
            state.updateSettings(AppSettings(theme = ThemeMode.LIGHT, editorFontSize = 15))

            withTimeout(3_000) {
                while (JsonSettingsRepository(paths).load().editorFontSize != 15) {
                    delay(20)
                }
            }
            val stored = JsonSettingsRepository(paths).load()
            assertEquals(ThemeMode.LIGHT, stored.theme)
            assertEquals(15, stored.editorFontSize)
            state.close()
        }

    @Test
    fun `closing flushes the last pending settings snapshot`() =
        runBlocking {
            val repository = RecordingSettingsRepository()
            val expected = AppSettings(theme = ThemeMode.LIGHT, editorFontSize = 17)
            val state =
                AppState(
                    container = TestContainer(repository),
                    scope = CoroutineScope(Dispatchers.Unconfined),
                    initialSettings = AppSettings(),
                    initialConnections = emptyList(),
                )

            state.updateSettings(expected)
            state.close()

            assertEquals(expected, repository.saved)
        }

    @Test
    fun `settings save failure marks the status and shows a toast`() =
        runBlocking {
            val failing =
                object : SettingsRepository {
                    override suspend fun load(): AppSettings = AppSettings()

                    override suspend fun save(settings: AppSettings) {
                        error("disk full")
                    }
                }
            val state =
                AppState(
                    container = TestContainer(failing),
                    scope = CoroutineScope(Dispatchers.Unconfined),
                    initialSettings = AppSettings(),
                    initialConnections = emptyList(),
                    settingsSaveDebounceMillis = 0,
                )

            state.updateSettings(AppSettings(theme = ThemeMode.DARK))

            assertEquals(SettingsSaveStatus.FAILED, state.settingsSaveStatus)
            assertTrue(state.toasts.any { it.kind == ToastKind.Danger })
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
