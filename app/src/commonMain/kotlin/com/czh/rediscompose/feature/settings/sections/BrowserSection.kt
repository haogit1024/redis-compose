package com.czh.rediscompose.feature.settings.sections

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.component.AppSegmented
import com.czh.rediscompose.design.component.AppSwitch
import com.czh.rediscompose.design.component.AppTextField
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.domain.settings.AppSettings
import com.czh.rediscompose.domain.settings.KeyView
import com.czh.rediscompose.feature.settings.SettingRow
import com.czh.rediscompose.feature.settings.SettingsNumberField
import com.czh.rediscompose.feature.settings.SettingsPanel
import com.czh.rediscompose.feature.settings.SettingsSection
import com.czh.rediscompose.feature.settings.SettingsSectionHeader
import com.czh.rediscompose.util.SettingsLimits

@Composable
internal fun BrowserSection(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
) {
    val dimens = LocalAppDimens.current
    SettingsSectionHeader(SettingsSection.BROWSER)
    SettingsPanel {
        SettingRow(
            label = "SCAN 批量大小",
            hint = "单次 SCAN 的 COUNT 值，过大可能阻塞服务器",
        ) {
            SettingsNumberField(
                value = settings.scanBatchSize,
                range = SettingsLimits.SCAN_BATCH_SIZE,
                onValueChange = {
                    onSettingsChange(settings.copy(scanBatchSize = SettingsLimits.scanBatchSize(it)))
                },
                width = 96.dp,
            )
        }
        SettingRow(
            label = "默认视图",
            hint = "按分隔符组织的树形结构更易浏览",
        ) {
            AppSegmented(
                options = KeyView.entries,
                selected = settings.keyView,
                onSelect = { onSettingsChange(settings.copy(keyView = it)) },
                label = { if (it == KeyView.TREE) "树形" else "平铺" },
            )
        }
        SettingRow(
            label = "Key 分隔符",
            hint = "新连接使用的默认分隔符；修改不会影响已有连接",
        ) {
            KeySeparatorField(
                value = settings.defaultKeySeparator,
                onValueChange = {
                    onSettingsChange(settings.copy(defaultKeySeparator = it))
                },
                width = 64.dp,
                height = dimens.controlHeightSmall,
            )
        }
        SettingRow(
            label = "显示 TTL 徽标",
            hint = "在 Key 列表中展示剩余生存时间",
        ) {
            AppSwitch(
                checked = settings.showKeyTtl,
                onCheckedChange = { onSettingsChange(settings.copy(showKeyTtl = it)) },
            )
        }
        SettingRow(
            label = "删除 Key 前确认",
            hint = "关闭后单个 Key 直接删除；批量删除未来仍会确认",
        ) {
            AppSwitch(
                checked = settings.confirmDeleteKeys,
                onCheckedChange = { onSettingsChange(settings.copy(confirmDeleteKeys = it)) },
            )
        }
        SettingRow(
            label = "自动刷新间隔",
            hint = "空闲时同步侧栏会话摘要与 DB 计数（不刷新 Key 列表）",
            last = true,
        ) {
            AppSegmented(
                options = AUTO_REFRESH_OPTIONS,
                selected = settings.autoRefreshSeconds,
                onSelect = { onSettingsChange(settings.copy(autoRefreshSeconds = it)) },
                label = { seconds ->
                    when (seconds) {
                        0 -> "关闭"
                        60 -> "60 秒"
                        else -> "30 秒"
                    }
                },
            )
        }
    }
}

@Composable
private fun KeySeparatorField(
    value: String,
    onValueChange: (String) -> Unit,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
) {
    var focused by remember { mutableStateOf(false) }
    var draft by remember(value) { mutableStateOf(value) }
    LaunchedEffect(value, focused) {
        if (!focused) draft = value
    }
    AppTextField(
        value = draft,
        onValueChange = { input ->
            draft = input.take(SettingsLimits.KEY_SEPARATOR_MAX_LENGTH)
            if (draft.isNotEmpty()) onValueChange(draft)
        },
        modifier = Modifier.width(width),
        mono = true,
        height = height,
        keyboardType = KeyboardType.Ascii,
        onFocusChanged = { isFocused ->
            focused = isFocused
            if (!isFocused) {
                val committed = SettingsLimits.keySeparator(draft)
                draft = committed
                if (committed != value) onValueChange(committed)
            }
        },
    )
}

private val AUTO_REFRESH_OPTIONS = listOf(0, 30, 60)

@Preview(widthDp = 760, heightDp = 480)
@Composable
private fun BrowserSectionPreview() {
    AppTheme(dark = true) {
        BrowserSection(settings = AppSettings(), onSettingsChange = {})
    }
}

@Preview(widthDp = 760, heightDp = 480)
@Composable
private fun BrowserSectionLightPreview() {
    AppTheme(dark = false) {
        BrowserSection(settings = AppSettings(keyView = KeyView.FLAT), onSettingsChange = {})
    }
}
