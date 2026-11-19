package com.czh.rediscompose.feature.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.app.AppPage
import com.czh.rediscompose.app.AppState
import com.czh.rediscompose.app.SessionStatus
import com.czh.rediscompose.app.SettingsSaveStatus
import com.czh.rediscompose.design.component.AppBadge
import com.czh.rediscompose.design.component.AppBadgeTone
import com.czh.rediscompose.design.component.AppIconButton
import com.czh.rediscompose.design.component.AppKbd
import com.czh.rediscompose.design.component.AppTooltip
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.icon.BrandMark
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.domain.settings.ThemeMode
import com.czh.rediscompose.util.ShortcutLabels

@Composable
fun AppTopBar(
    state: AppState,
    dark: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val connection = state.activeConnection
    val session = state.activeSession()

    Column(modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(dimens.topBarHeight)
                    .background(colors.surface)
                    .padding(start = dimens.spaceLg, end = dimens.spaceLg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceLg),
        ) {
            Brand()
            Box(
                Modifier
                    .width(1.dp)
                    .height(dimens.topBarSeparatorHeight)
                    .background(colors.border),
            )
            PageChip(state = state, connectionName = connection?.name)
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Omnibox(onClick = state::toggleCommandPalette)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.iconTextGap),
            ) {
                if (state.activePage == AppPage.SETTINGS) {
                    SettingsSavePill(state.settingsSaveStatus)
                }
                StatusPill(session = session)
                AppTooltip(if (dark) "切换到浅色主题" else "切换到深色主题") {
                    AppIconButton(
                        icon = if (dark) AppIcons.ThemeLight else AppIcons.ThemeDark,
                        contentDescription = if (dark) "切换到浅色主题" else "切换到深色主题",
                        onClick = {
                            state.updateTheme(if (dark) ThemeMode.LIGHT else ThemeMode.DARK)
                        },
                    )
                }
                AppTooltip("通知") {
                    AppIconButton(
                        icon = AppIcons.Bell,
                        contentDescription = "通知",
                        onClick = { state.showToast("暂无通知") },
                    )
                }
                AppTooltip("本地工作区") {
                    Box(
                        modifier =
                            Modifier
                                .size(dimens.avatarSize)
                                .background(colors.accentSoft, CircleShape)
                                .border(1.dp, colors.accentLine, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "RC",
                            style = typography.badge,
                            color = colors.accent,
                        )
                    }
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.border),
        )
    }
}

@Composable
private fun Brand() {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val dimens = LocalAppDimens.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
    ) {
        BrandMark()
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "redis",
                style = typography.bodyStrong,
                color = colors.text,
            )
            Text(
                text = "compose",
                style = typography.body,
                color = colors.textTertiary,
                modifier = Modifier.padding(start = 3.dp),
            )
        }
        Text(
            text = "v0.1",
            style = typography.monoMeta,
            color = colors.textQuaternary,
            modifier =
                Modifier
                    .border(1.dp, colors.border, RoundedCornerShape(dimens.radiusXs))
                    .padding(horizontal = dimens.spaceXs, vertical = 1.dp),
        )
    }
}

@Composable
private fun PageChip(
    state: AppState,
    connectionName: String?,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val connection = state.activeConnection
    val page = state.activePage
    val shape = RoundedCornerShape(dimens.radiusSm)
    val (chipIcon, chipLabel) =
        when (page) {
            AppPage.SETTINGS -> AppIcons.Settings to "偏好设置"
            AppPage.CONNECTIONS -> AppIcons.Connections to "连接库"
            AppPage.WORKSPACE -> AppIcons.Database to (connection?.environment?.displayName() ?: "未选择连接")
        }
    Row(
        modifier =
            Modifier
                .height(dimens.pageChipHeight)
                .background(if (hovered) colors.surface2 else Color.Transparent, shape)
                .border(1.dp, if (hovered) colors.border else Color.Transparent, shape)
                .hoverable(interaction)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                ) {
                    state.setPage(if (connection == null) AppPage.CONNECTIONS else AppPage.WORKSPACE)
                }
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(horizontal = dimens.fieldPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
    ) {
        Icon(
            imageVector = chipIcon,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(dimens.iconSm),
        )
        Text(
            text = chipLabel,
            style = typography.body,
            color = if (connection == null && page == AppPage.WORKSPACE) colors.textTertiary else colors.textSecondary,
        )
        if (page == AppPage.WORKSPACE && connectionName != null) {
            Text(
                text = "/",
                style = typography.body,
                color = colors.textQuaternary,
            )
            Text(
                text = connectionName,
                style = typography.bodyStrong,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = dimens.breadcrumbMaxNameWidth),
            )
        }
        Icon(
            imageVector = AppIcons.ChevronDown,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(dimens.iconSm),
        )
    }
}

@Composable
private fun SettingsSavePill(status: SettingsSaveStatus) {
    val (text, tone, icon) =
        when (status) {
            SettingsSaveStatus.SAVED -> Triple("已自动保存", AppBadgeTone.Success, AppIcons.Check)
            SettingsSaveStatus.SAVING -> Triple("保存中…", AppBadgeTone.Warning, AppIcons.Refresh)
            SettingsSaveStatus.FAILED -> Triple("未保存", AppBadgeTone.Danger, AppIcons.ErrorCircle)
        }
    AppBadge(text = text, tone = tone, icon = icon)
}

@Composable
private fun Omnibox(onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(dimens.radiusControl + 1.dp)
    Row(
        modifier =
            Modifier
                .widthIn(max = dimens.omniboxMaxWidth)
                .fillMaxWidth()
                .height(dimens.controlHeight)
                .background(colors.surface2, shape)
                .border(1.dp, if (hovered) colors.borderStrong else colors.border, shape)
                .hoverable(interaction)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                )
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(start = dimens.fieldPadding, end = dimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
    ) {
        Icon(
            imageVector = AppIcons.Search,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(dimens.iconSm),
        )
        Text(
            text = "搜索 Key、命令或连接…",
            style = typography.bodySmall,
            color = colors.textTertiary,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        AppKbd(ShortcutLabels.commandKey("K"))
    }
}

@Composable
private fun StatusPill(session: com.czh.rediscompose.app.RedisSession?) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val (dotColor, label) =
        when (session?.status) {
            SessionStatus.CONNECTED -> colors.success to (session.latencyMillis?.let { "$it ms" } ?: "已连接")
            SessionStatus.CONNECTING -> colors.warning to "连接中"
            SessionStatus.ERROR -> colors.danger to "连接失败"
            else -> colors.textQuaternary to "未连接"
        }
    Row(
        modifier =
            Modifier
                .height(dimens.statusPillHeight)
                .background(colors.surface2, RoundedCornerShape(dimens.pillRadius))
                .border(1.dp, colors.border, RoundedCornerShape(dimens.pillRadius))
                .padding(horizontal = dimens.spaceSm + 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.iconTextGap),
    ) {
        Box(
            Modifier
                .size(dimens.dotSize)
                .background(dotColor, CircleShape),
        )
        Text(
            text = label,
            style = typography.monoValue,
            color = colors.textSecondary,
            maxLines = 1,
        )
    }
}

@Preview(widthDp = 1440, heightDp = 60)
@Composable
private fun AppTopBarPreview() {
    AppTheme(dark = true) {
        AppTopBar(
            state = previewEmptyState(),
            dark = true,
        )
    }
}
