package com.czh.rediscompose.feature.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.component.AppKbd
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.feature.settings.SettingsPanel
import com.czh.rediscompose.feature.settings.SettingsSection
import com.czh.rediscompose.feature.settings.SettingsSectionHeader
import com.czh.rediscompose.util.ShortcutLabels

private data class ShortcutRow(
    val action: String,
    val keys: List<String>,
)

/** Read-only table of shortcuts that are actually implemented in this build. */
@Composable
internal fun ShortcutsSection() {
    val dimens = LocalAppDimens.current
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val command = ShortcutLabels.command
    val shift = ShortcutLabels.shift
    val rows =
        listOf(
            ShortcutRow("打开命令面板", listOf(command, "K")),
            ShortcutRow("搜索当前 Keyspace", listOf(command, "F")),
            ShortcutRow("保存当前值", listOf(command, "S")),
            ShortcutRow("执行控制台命令", listOf(command, "Enter")),
            ShortcutRow("新建 Key", listOf("N")),
            ShortcutRow("刷新当前 Keyspace", listOf("R")),
            ShortcutRow("历史命令上一条 / 下一条", listOf("↑", "↓")),
            ShortcutRow("关闭当前标签页", listOf(command, "W")),
            ShortcutRow("切换深 / 浅主题", listOf(command, shift, "L")),
            ShortcutRow("打开设置", listOf(command, ",")),
            ShortcutRow("关闭弹层 / 命令面板", listOf("Esc")),
        )
    SettingsSectionHeader(SettingsSection.SHORTCUTS)
    SettingsPanel {
        rows.forEachIndexed { index, row ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            if (index != rows.lastIndex) {
                                drawRect(
                                    color = colors.border,
                                    topLeft = Offset(0f, size.height - 1.dp.toPx()),
                                    size = Size(size.width, 1.dp.toPx()),
                                )
                            }
                        }
                        .height(dimens.settings.shortcutRowHeight)
                        .padding(horizontal = dimens.settings.shortcutCellPaddingH),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = row.action,
                    style = typography.bodySmall,
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(dimens.settings.shortcutKeysGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    row.keys.forEach { key -> AppKbd(key) }
                }
            }
        }
        Column(Modifier.padding(horizontal = dimens.settings.shortcutCellPaddingH, vertical = dimens.spaceSm)) {
            Text(
                text = "v1 不支持自定义键位；Windows / Linux 使用 Ctrl。",
                style = typography.caption,
                color = colors.textQuaternary,
            )
        }
    }
}

@Preview(widthDp = 760, heightDp = 560)
@Composable
private fun ShortcutsSectionPreview() {
    AppTheme(dark = true) {
        ShortcutsSection()
    }
}

@Preview(widthDp = 760, heightDp = 560)
@Composable
private fun ShortcutsSectionLightPreview() {
    AppTheme(dark = false) {
        ShortcutsSection()
    }
}
