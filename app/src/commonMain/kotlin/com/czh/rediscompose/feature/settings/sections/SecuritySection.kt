package com.czh.rediscompose.feature.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.component.AppBadge
import com.czh.rediscompose.design.component.AppBadgeTone
import com.czh.rediscompose.design.component.AppButton
import com.czh.rediscompose.design.component.AppButtonSize
import com.czh.rediscompose.design.component.AppButtonVariant
import com.czh.rediscompose.design.component.AppSwitch
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.domain.settings.AppSettings
import com.czh.rediscompose.feature.settings.SettingRow
import com.czh.rediscompose.feature.settings.SettingsPanel
import com.czh.rediscompose.feature.settings.SettingsSection
import com.czh.rediscompose.feature.settings.SettingsSectionHeader

@Composable
internal fun SecuritySection(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onClearCacheRequest: () -> Unit,
) {
    SettingsSectionHeader(SettingsSection.SECURITY)
    SettingsPanel {
        SettingRow(
            label = "全局只读模式",
            hint = "拦截所有连接上的写命令（SET / DEL / FLUSHDB 等），状态栏会显示只读徽标",
        ) {
            AppSwitch(
                checked = settings.globalReadOnly,
                onCheckedChange = { onSettingsChange(settings.copy(globalReadOnly = it)) },
            )
        }
        SettingRow(
            label = "加密存储密码",
            hint =
                if (settings.encryptPasswords) {
                    "使用 AES-256-GCM 加密本地连接密码"
                } else {
                    "已关闭：下次保存连接时以明文写入本地文件，存在泄露风险"
                },
        ) {
            if (!settings.encryptPasswords) {
                AppBadge(text = "明文风险", tone = AppBadgeTone.Warning, icon = AppIcons.Warning)
            }
            AppSwitch(
                checked = settings.encryptPasswords,
                onCheckedChange = { onSettingsChange(settings.copy(encryptPasswords = it)) },
            )
        }
        SettingRow(
            label = "隐藏敏感值",
            hint = "自动遮罩字段名包含 password / secret / token 的 Hash 值，可逐行显示",
        ) {
            AppSwitch(
                checked = settings.maskSensitiveValues,
                onCheckedChange = { onSettingsChange(settings.copy(maskSensitiveValues = it)) },
            )
        }
        SettingRow(
            label = "危险命令二次确认",
            hint = "FLUSHALL / FLUSHDB / CONFIG SET / EVAL 等操作需手动确认",
            last = true,
        ) {
            AppSwitch(
                checked = settings.confirmDangerousCommands,
                onCheckedChange = { onSettingsChange(settings.copy(confirmDangerousCommands = it)) },
            )
        }
    }
    DangerZone(onClearCacheRequest = onClearCacheRequest)
}

@Composable
private fun DangerZone(onClearCacheRequest: () -> Unit) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val shape = RoundedCornerShape(dimens.settings.panelRadius)
    Row(
        modifier =
            Modifier
                .padding(top = dimens.spaceSm + 2.dp)
                .fillMaxWidth()
                .background(colors.surface, shape)
                .border(1.dp, colors.danger.copy(alpha = 0.22f), shape)
                .padding(horizontal = dimens.settings.rowPaddingH, vertical = dimens.settings.rowPaddingV)
                .defaultMinSize(minHeight = dimens.settings.rowMinHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.settings.rowGap),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "清除本地缓存",
                style = typography.settingsLabel,
                color = colors.danger,
            )
            Text(
                text = "删除命令历史、最近的 Key 筛选与本地指标样本；不会影响 Redis 服务器或连接配置",
                style = typography.settingsHint,
                color = colors.textTertiary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        AppButton(
            text = "清除缓存",
            onClick = onClearCacheRequest,
            variant = AppButtonVariant.Danger,
            size = AppButtonSize.Small,
            icon = AppIcons.Delete,
        )
    }
}

@Preview(widthDp = 760, heightDp = 520)
@Composable
private fun SecuritySectionPreview() {
    AppTheme(dark = true) {
        SecuritySection(settings = AppSettings(), onSettingsChange = {}, onClearCacheRequest = {})
    }
}

@Preview(widthDp = 760, heightDp = 520)
@Composable
private fun SecuritySectionLightPreview() {
    AppTheme(dark = false) {
        SecuritySection(
            settings = AppSettings(encryptPasswords = false, maskSensitiveValues = true),
            onSettingsChange = {},
            onClearCacheRequest = {},
        )
    }
}
