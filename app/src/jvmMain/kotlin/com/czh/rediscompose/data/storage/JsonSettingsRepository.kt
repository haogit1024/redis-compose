package com.czh.rediscompose.data.storage

import com.czh.rediscompose.domain.settings.AppSettings
import com.czh.rediscompose.domain.settings.KeyView
import com.czh.rediscompose.domain.settings.SettingsRepository
import com.czh.rediscompose.domain.settings.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
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
                if (text.isBlank()) AppSettings() else decodeSettings(text)
            }.getOrElse { error ->
                println("[redis-compose] failed to read settings: ${error.firstLine()}")
                AppSettings()
            }
        }

    override suspend fun save(settings: AppSettings) {
        withContext(Dispatchers.IO) {
            writeAtomically(paths.settings, json.encodeToString(settings))
        }
    }

    private fun Throwable.firstLine(): String = message?.lineSequence()?.firstOrNull() ?: this::class.simpleName.orEmpty()

    private fun decodeSettings(text: String): AppSettings {
        val modern = runCatching { json.decodeFromString<AppSettings>(text) }.getOrNull()
        if (modern != null) return modern
        return runCatching { json.decodeFromString<LegacySettings>(text).toAppSettings() }
            .getOrElse { AppSettings() }
    }
}

/**
 * Settings written by the pre-rewrite app (`shared/src/.../AppSettings.kt`).
 *
 * The old file used a lowercase theme string and different field names; reading it here
 * keeps theme/editor preferences without a migration step.
 */
@Serializable
private data class LegacySettings(
    val theme: String = "system",
    val fontFamily: String = "JetBrains Mono",
    val fontSize: Int = 13,
    val batchSize: Int = 1_000,
    val valueTruncateKb: Int = 10,
    val defaultView: String = "tree",
    val readonly: Boolean = false,
    val encryptPassword: Boolean = true,
) {
    fun toAppSettings(): AppSettings =
        AppSettings(
            theme =
                when (theme.lowercase()) {
                    "light" -> ThemeMode.LIGHT
                    "dark" -> ThemeMode.DARK
                    else -> ThemeMode.SYSTEM
                },
            editorFont = fontFamily.ifBlank { "JetBrains Mono" },
            editorFontSize = fontSize,
            scanBatchSize = batchSize,
            truncateValueKb = valueTruncateKb,
            keyView = if (defaultView.equals("flat", ignoreCase = true)) KeyView.FLAT else KeyView.TREE,
            globalReadOnly = readonly,
            encryptPasswords = encryptPassword,
        )
}
