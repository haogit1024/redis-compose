package com.czh.rediscompose.feature.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.czh.rediscompose.design.component.AppBadge
import com.czh.rediscompose.design.component.AppBadgeTone
import com.czh.rediscompose.design.component.AppSegmented
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.domain.settings.AppSettings
import com.czh.rediscompose.domain.settings.ThemeMode
import com.czh.rediscompose.domain.settings.UiDensity
import com.czh.rediscompose.feature.settings.AccentSwatches
import com.czh.rediscompose.feature.settings.SettingRow
import com.czh.rediscompose.feature.settings.SettingsDivider
import com.czh.rediscompose.feature.settings.SettingsPanel
import com.czh.rediscompose.feature.settings.SettingsPreviewFrame
import com.czh.rediscompose.feature.settings.SettingsSection
import com.czh.rediscompose.feature.settings.SettingsSectionHeader
import com.czh.rediscompose.feature.settings.SettingsStaticValue
import com.czh.rediscompose.feature.settings.ThemeChoiceCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AppearanceSection(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
) {
    val dimens = LocalAppDimens.current
    SettingsSectionHeader(SettingsSection.APPEARANCE)
    SettingsPanel {
        FlowRow(
            modifier =
                Modifier
                    .padding(dimens.settings.themeCardsPadding),
            horizontalArrangement = Arrangement.spacedBy(dimens.settings.themeCardsGap),
            verticalArrangement = Arrangement.spacedBy(dimens.settings.themeCardsGap),
        ) {
            THEME_CARD_ORDER.forEach { mode ->
                ThemeChoiceCard(
                    choice = mode,
                    selected = settings.theme == mode,
                    onClick = { onSettingsChange(settings.copy(theme = mode)) },
                    accent = settings.accent,
                )
            }
        }
        SettingsDivider()
        SettingRow(
            label = "强调色",
            hint = "影响按钮、选中态与图表主色",
        ) {
            AccentSwatches(
                selected = settings.accent,
                onSelect = { onSettingsChange(settings.copy(accent = it)) },
            )
        }
        SettingRow(
            label = "界面语言",
            hint = "部分技术术语保持英文",
        ) {
            SettingsStaticValue("简体中文")
            AppBadge(text = "v1 仅此一项", tone = AppBadgeTone.Plain)
        }
        SettingRow(
            label = "界面密度",
            hint = "调整列表、表格与设置行高",
            last = true,
        ) {
            AppSegmented(
                options = UiDensity.entries,
                selected = settings.density,
                onSelect = { onSettingsChange(settings.copy(density = it)) },
                label = { if (it == UiDensity.COMFORTABLE) "舒适" else "紧凑" },
            )
        }
    }
}

private val THEME_CARD_ORDER = listOf(ThemeMode.DARK, ThemeMode.LIGHT, ThemeMode.SYSTEM)

@Preview(widthDp = 760, heightDp = 520)
@Composable
private fun AppearanceSectionPreview() {
    AppTheme(dark = true) {
        SettingsPreviewFrame {
            AppearanceSection(settings = AppSettings(theme = ThemeMode.SYSTEM), onSettingsChange = {})
        }
    }
}

@Preview(widthDp = 760, heightDp = 520)
@Composable
private fun AppearanceSectionLightPreview() {
    AppTheme(dark = false) {
        SettingsPreviewFrame {
            AppearanceSection(settings = AppSettings(theme = ThemeMode.LIGHT), onSettingsChange = {})
        }
    }
}
