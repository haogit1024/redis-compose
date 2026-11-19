package com.czh.rediscompose.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class SettingsGroup(val label: String) {
    PREFERENCES("偏好"),
    SYSTEM("系统"),
}

/** Navigation entries for the settings page, in scroll order. */
enum class SettingsSection(
    val group: SettingsGroup,
    val title: String,
    val description: String,
) {
    APPEARANCE(SettingsGroup.PREFERENCES, "外观", "主题、强调色与语言"),
    EDITOR(SettingsGroup.PREFERENCES, "编辑器与值展示", "控制 Key 值的编辑体验"),
    BROWSER(SettingsGroup.PREFERENCES, "键浏览", "Keyspace 加载与展示参数"),
    SECURITY(SettingsGroup.SYSTEM, "安全", "写入保护与本地凭据"),
    SHORTCUTS(SettingsGroup.SYSTEM, "快捷键", "全局与工作区快捷键"),
    ABOUT(SettingsGroup.SYSTEM, "关于", "版本与开源信息"),
}

/**
 * Local UI state for the settings page.
 *
 * Persisted values live in `AppState`; this holder only owns which section is active and
 * whether the danger-zone confirmation is open.
 */
class SettingsState {
    var activeSection by mutableStateOf(SettingsSection.APPEARANCE)
        private set

    var clearCacheDialogVisible by mutableStateOf(false)
        private set

    fun select(section: SettingsSection) {
        activeSection = section
    }

    fun requestClearCache() {
        clearCacheDialogVisible = true
    }

    fun dismissClearCache() {
        clearCacheDialogVisible = false
    }
}
