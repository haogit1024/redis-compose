package com.czh.rediscompose.domain.settings

import kotlinx.serialization.Serializable

@Serializable
enum class KeyView { TREE, FLAT }

@Serializable
enum class ValueFormat { AUTO, JSON, TEXT, HEX }

@Serializable
data class AppSettings(
    val schemaVersion: Int = 1,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val accent: Accent = Accent.IRIS,
    val density: UiDensity = UiDensity.COMFORTABLE,
    val uiFont: String = "Inter",
    val editorFont: String = "JetBrains Mono",
    val editorFontSize: Int = 13,
    val defaultValueFormat: ValueFormat = ValueFormat.AUTO,
    val wordWrap: Boolean = true,
    val showLineNumbers: Boolean = true,
    val scanBatchSize: Int = 1_000,
    val truncateValueKb: Int = 10,
    val keyView: KeyView = KeyView.TREE,
    val defaultKeySeparator: String = ":",
    val showKeyTtl: Boolean = true,
    val confirmDeleteKeys: Boolean = true,
    val confirmDangerousCommands: Boolean = true,
    val maskSensitiveValues: Boolean = false,
    val globalReadOnly: Boolean = false,
    val encryptPasswords: Boolean = true,
    val autoRefreshSeconds: Int = 30,
    val recentCommands: List<String> = emptyList(),
)
