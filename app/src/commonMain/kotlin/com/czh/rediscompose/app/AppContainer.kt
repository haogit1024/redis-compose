package com.czh.rediscompose.app

import com.czh.rediscompose.data.redis.RedisGatewayFactory
import com.czh.rediscompose.domain.connection.ConnectionProfile
import com.czh.rediscompose.domain.connection.ConnectionRepository
import com.czh.rediscompose.domain.repository.PasswordCipher
import com.czh.rediscompose.domain.settings.AppSettings
import com.czh.rediscompose.domain.settings.SettingsRepository

/**
 * Composition root for the app.
 *
 * `AppState` is built from this container; UI code never constructs repositories
 * or gateways itself.
 */
interface AppContainer {
    val settingsRepository: SettingsRepository
    val connectionRepository: ConnectionRepository
    val gatewayFactory: RedisGatewayFactory
    val passwordCipher: PasswordCipher
    val initialSettings: AppSettings
    val initialConnections: List<ConnectionProfile>
}
