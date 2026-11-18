package com.czh.rediscompose.domain.settings

interface SettingsRepository {
    suspend fun load(): AppSettings

    suspend fun save(settings: AppSettings)
}
