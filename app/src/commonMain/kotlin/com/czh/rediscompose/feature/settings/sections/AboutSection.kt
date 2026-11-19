package com.czh.rediscompose.feature.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.component.AppBadge
import com.czh.rediscompose.design.component.AppBadgeTone
import com.czh.rediscompose.design.component.AppButton
import com.czh.rediscompose.design.component.AppButtonSize
import com.czh.rediscompose.design.component.AppButtonVariant
import com.czh.rediscompose.design.component.ToastKind
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.icon.BrandMark
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.feature.settings.SettingRow
import com.czh.rediscompose.feature.settings.SettingsPanel
import com.czh.rediscompose.feature.settings.SettingsSection
import com.czh.rediscompose.feature.settings.SettingsSectionHeader

private const val CONFIG_PATH = "~/.redis-compose/"
private const val REPOSITORY_URL = "https://github.com/haogit1024/redis-compose"

@Composable
internal fun AboutSection(onToast: (String, ToastKind) -> Unit) {
    val dimens = LocalAppDimens.current
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val openRepository = { uriHandler.openUri(REPOSITORY_URL) }
    SettingsSectionHeader(SettingsSection.ABOUT)
    SettingsPanel {
        AboutCard(onOpenRepository = openRepository)
        SettingRow(
            label = "配置位置",
            hint = "设置、连接与密钥文件都保存在此目录",
        ) {
            Text(
                text = CONFIG_PATH,
                style = LocalAppTypography.current.aboutMono,
                color = LocalAppColors.current.textSecondary,
            )
            AppButton(
                text = "复制路径",
                onClick = {
                    clipboard.setText(AnnotatedString(CONFIG_PATH))
                    onToast("已复制配置路径", ToastKind.Success)
                },
                variant = AppButtonVariant.Ghost,
                size = AppButtonSize.Small,
                icon = AppIcons.Copy,
            )
        }
        SettingRow(
            label = "参与贡献",
            hint = "发现问题或有新的功能想法？欢迎提交 Issue 或 PR。",
            last = true,
        ) {
            AppButton(
                text = "打开仓库",
                onClick = openRepository,
                variant = AppButtonVariant.Secondary,
                size = AppButtonSize.Small,
                icon = AppIcons.External,
            )
        }
    }
    Column(Modifier.padding(top = dimens.spaceSm)) {
        Text(
            text = "Redis Compose 0.1.0 · 本地优先的 Redis 工作台",
            style = LocalAppTypography.current.caption,
            color = LocalAppColors.current.textQuaternary,
        )
    }
}

@Composable
private fun AboutCard(onOpenRepository: () -> Unit) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(dimens.settings.aboutCardPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceMd + 1.dp),
    ) {
        BrandMark(size = dimens.settings.aboutLogoSize)
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
            ) {
                Text(
                    text = "Redis Compose",
                    style = typography.cardTitle,
                    color = colors.text,
                )
                AppBadge(text = "0.1.0", tone = AppBadgeTone.Accent)
            }
            Text(
                text = "Kotlin Multiplatform · Compose Desktop",
                style = typography.aboutMono,
                color = colors.textTertiary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        AppButton(
            text = "GitHub",
            onClick = onOpenRepository,
            variant = AppButtonVariant.Ghost,
            size = AppButtonSize.Small,
            icon = AppIcons.External,
        )
    }
}

@Preview(widthDp = 760, heightDp = 360)
@Composable
private fun AboutSectionPreview() {
    AppTheme(dark = true) {
        AboutSection(onToast = { _, _ -> })
    }
}

@Preview(widthDp = 760, heightDp = 360)
@Composable
private fun AboutSectionLightPreview() {
    AppTheme(dark = false) {
        AboutSection(onToast = { _, _ -> })
    }
}
