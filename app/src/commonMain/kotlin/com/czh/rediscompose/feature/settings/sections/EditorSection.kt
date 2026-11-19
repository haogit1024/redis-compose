package com.czh.rediscompose.feature.settings.sections

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.component.AppBadge
import com.czh.rediscompose.design.component.AppBadgeTone
import com.czh.rediscompose.design.component.AppSegmented
import com.czh.rediscompose.design.component.AppSwitch
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.domain.settings.AppSettings
import com.czh.rediscompose.domain.settings.ValueFormat
import com.czh.rediscompose.feature.settings.SettingRow
import com.czh.rediscompose.feature.settings.SettingsNumberField
import com.czh.rediscompose.feature.settings.SettingsPanel
import com.czh.rediscompose.feature.settings.SettingsSection
import com.czh.rediscompose.feature.settings.SettingsSectionHeader
import com.czh.rediscompose.feature.settings.SettingsStaticValue
import com.czh.rediscompose.feature.settings.SettingsStepper
import com.czh.rediscompose.util.SettingsLimits

@Composable
internal fun EditorSection(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
) {
    SettingsSectionHeader(SettingsSection.EDITOR)
    SettingsPanel {
        SettingRow(
            label = "默认值格式",
            hint = "打开 String 值时使用的展示模式",
        ) {
            AppSegmented(
                options = ValueFormat.entries,
                selected = settings.defaultValueFormat,
                onSelect = { onSettingsChange(settings.copy(defaultValueFormat = it)) },
                label = { valueFormatLabel(it) },
            )
        }
        SettingRow(
            label = "等宽字体",
            hint = "用于 Key、值与命令控制台",
        ) {
            SettingsStaticValue("JetBrains Mono")
            AppBadge(text = "v1 唯一", tone = AppBadgeTone.Plain)
        }
        SettingRow(
            label = "字号",
            hint = "值编辑器的代码字号",
        ) {
            SettingsStepper(
                value = settings.editorFontSize,
                range = SettingsLimits.EDITOR_FONT_SIZE,
                onValueChange = {
                    onSettingsChange(settings.copy(editorFontSize = SettingsLimits.editorFontSize(it)))
                },
            )
        }
        SettingRow(
            label = "自动换行",
            hint = "长值超出宽度时换行显示",
        ) {
            AppSwitch(
                checked = settings.wordWrap,
                onCheckedChange = { onSettingsChange(settings.copy(wordWrap = it)) },
            )
        }
        SettingRow(
            label = "显示行号",
            hint = "在 JSON / 文本编辑器中显示行号",
        ) {
            AppSwitch(
                checked = settings.showLineNumbers,
                onCheckedChange = { onSettingsChange(settings.copy(showLineNumbers = it)) },
            )
        }
        SettingRow(
            label = "大值截断阈值",
            hint = "超过该大小时仅展示前段，避免卡顿（KB）",
            last = true,
        ) {
            SettingsNumberField(
                value = settings.truncateValueKb,
                range = SettingsLimits.TRUNCATE_VALUE_KB,
                onValueChange = {
                    onSettingsChange(settings.copy(truncateValueKb = SettingsLimits.truncateValueKb(it)))
                },
                width = 84.dp,
            )
        }
    }
}

private fun valueFormatLabel(format: ValueFormat): String =
    when (format) {
        ValueFormat.AUTO -> "Auto"
        ValueFormat.JSON -> "JSON"
        ValueFormat.TEXT -> "Text"
        ValueFormat.HEX -> "HEX"
    }

@Preview(widthDp = 760, heightDp = 480)
@Composable
private fun EditorSectionPreview() {
    AppTheme(dark = true) {
        EditorSection(settings = AppSettings(), onSettingsChange = {})
    }
}

@Preview(widthDp = 760, heightDp = 480)
@Composable
private fun EditorSectionLightPreview() {
    AppTheme(dark = false) {
        EditorSection(settings = AppSettings(editorFontSize = 16), onSettingsChange = {})
    }
}
