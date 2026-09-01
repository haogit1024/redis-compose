package com.czh.redis.data

import kotlinx.serialization.json.Json
import java.io.File

private val settingsFile: File
    get() = File(System.getProperty("user.home"), ".redis-compose/settings.json")

private val json =
    Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

actual fun loadSettings(): AppSettings {
    val file = settingsFile
    if (!file.exists()) return AppSettings()
    return runCatching {
        json.decodeFromString(AppSettings.serializer(), file.readText())
    }.getOrDefault(AppSettings())
}

actual fun saveSettings(settings: AppSettings) {
    runCatching {
        settingsFile.parentFile?.mkdirs()
        settingsFile.writeText(json.encodeToString(AppSettings.serializer(), settings))
    }
}
