package com.czh.rediscompose

import com.czh.rediscompose.app.AppContainer
import com.czh.rediscompose.data.jedis.JedisGatewayFactory
import com.czh.rediscompose.data.redis.RedisGatewayFactory
import com.czh.rediscompose.data.storage.AesGcmPasswordCipher
import com.czh.rediscompose.data.storage.FilePaths
import com.czh.rediscompose.data.storage.JsonConnectionRepository
import com.czh.rediscompose.data.storage.JsonSettingsRepository
import com.czh.rediscompose.domain.connection.ConnectionProfile
import com.czh.rediscompose.domain.connection.ConnectionRepository
import com.czh.rediscompose.domain.repository.PasswordCipher
import com.czh.rediscompose.domain.settings.AppSettings
import com.czh.rediscompose.domain.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * JVM composition root used by `Main.kt`.
 *
 * Initial settings/connections are read synchronously (the files are tiny) so the first
 * frame can render the correct theme and connection list.
 */
fun createDesktopAppContainer(): AppContainer {
    val paths = FilePaths()
    val cipher = AesGcmPasswordCipher(paths)
    val settingsRepository = JsonSettingsRepository(paths)
    val connectionRepository = JsonConnectionRepository(paths, cipher)
    val initialSettings = runBlocking(Dispatchers.IO) { settingsRepository.load() }
    val initialConnections = runBlocking(Dispatchers.IO) { connectionRepository.load() }
    return DesktopAppContainer(
        settingsRepository = settingsRepository,
        connectionRepository = connectionRepository,
        gatewayFactory = JedisGatewayFactory(),
        passwordCipher = cipher,
        initialSettings = initialSettings,
        initialConnections = initialConnections,
    )
}

private class DesktopAppContainer(
    override val settingsRepository: SettingsRepository,
    override val connectionRepository: ConnectionRepository,
    override val gatewayFactory: RedisGatewayFactory,
    override val passwordCipher: PasswordCipher,
    override val initialSettings: AppSettings,
    override val initialConnections: List<ConnectionProfile>,
) : AppContainer
