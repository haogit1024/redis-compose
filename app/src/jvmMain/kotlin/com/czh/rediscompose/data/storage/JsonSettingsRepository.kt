package com.czh.rediscompose.data.storage

import com.czh.rediscompose.domain.settings.AppSettings
import com.czh.rediscompose.domain.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files

class JsonSettingsRepository(
    private val paths: FilePaths = FilePaths(),
    private val json: Json = storageJson,
) : SettingsRepository {
    override suspend fun load(): AppSettings =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!Files.exists(paths.settings)) return@runCatching AppSettings()
                val text = Files.readString(paths.settings)
                if (text.isBlank()) AppSettings() else json.decodeFromString<AppSettings>(text)
            }.getOrElse { error ->
                println("[redis-compose] failed to read settings: ${error.firstLine()}")
                AppSettings()
            }
        }

    override suspend fun save(settings: AppSettings) {
        withContext(Dispatchers.IO) {
            runCatching {
                writeAtomically(paths.settings, json.encodeToString(settings))
            }.onFailure { error ->
                println("[redis-compose] failed to write settings: ${error.firstLine()}")
            }
        }
    }

    private fun Throwable.firstLine(): String = message?.lineSequence()?.firstOrNull() ?: this::class.simpleName.orEmpty()
}
