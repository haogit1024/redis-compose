package com.czh.rediscompose.feature.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.app.AppPage
import com.czh.rediscompose.app.AppState
import com.czh.rediscompose.app.WorkspaceTab
import com.czh.rediscompose.app.WorkspaceTabKind
import com.czh.rediscompose.design.component.AppBadge
import com.czh.rediscompose.design.component.AppBadgeTone
import com.czh.rediscompose.design.component.AppIconButton
import com.czh.rediscompose.design.component.AppMenu
import com.czh.rediscompose.design.component.AppMenuItem
import com.czh.rediscompose.design.component.AppTooltip
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.util.formatCount

@Composable
fun WorkspaceTabBar(
    state: AppState,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val activeTab = state.activeTab()
    val session = state.activeSession()

    Box(modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(dimens.tabBarHeight)
                    .background(colors.surface)
                    .padding(start = dimens.spaceMd, end = dimens.spaceMd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs),
        ) {
            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs),
            ) {
                state.tabs.forEach { tab ->
                    TabItem(
                        tab = tab,
                        active = tab.id == state.activeTabId,
                        onSelect = { state.selectTab(tab.id) },
                        onClose = { state.closeTab(tab.id) },
                    )
                }
                AddTabButton(onClick = { state.setPage(AppPage.CONNECTIONS) })
            }
            if (activeTab != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs + 1.dp),
                ) {
                    DatabasePill(
                        state = state,
                        enabled = session?.isConnected == true,
                    )
                    val readOnly = state.settings.globalReadOnly || state.activeConnection?.readOnly == true
                    AppBadge(
                        text = if (readOnly) "只读" else "可写",
                        tone = if (readOnly) AppBadgeTone.Warning else AppBadgeTone.Plain,
                        icon = AppIcons.Shield,
                    )
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .width(1.dp)
                            .height(dimens.tabDividerHeight)
                            .background(colors.border),
                    )
                    AppTooltip("刷新") {
                        AppIconButton(
                            icon = AppIcons.Refresh,
                            contentDescription = "刷新",
                            onClick = { state.showToast("刷新将在 Phase 4 接入") },
                        )
                    }
                    MoreMenu(state = state)
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
private fun TabItem(
    tab: WorkspaceTab,
    active: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(dimens.radiusSm)
    val icon: ImageVector =
        when (tab.kind) {
            WorkspaceTabKind.KEYS -> AppIcons.Key
            WorkspaceTabKind.CONSOLE -> AppIcons.Console
            WorkspaceTabKind.INSIGHTS -> AppIcons.Insights
        }
    Row(
        modifier =
            Modifier
                .height(dimens.tabHeight)
                .widthIn(max = dimens.tabMaxWidth)
                .clip(shape)
                .background(
                    if (active) {
                        colors.surface3
                    } else if (hovered) {
                        colors.surface2
                    } else {
                        Color.Transparent
                    },
                )
                .border(1.dp, if (active) colors.border else Color.Transparent, shape)
                .hoverable(interaction)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onSelect,
                )
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(start = dimens.fieldPadding, end = if (hovered || active) dimens.spaceXs else dimens.fieldPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm - 1.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) colors.accent else colors.textTertiary,
            modifier = Modifier.size(dimens.iconSm),
        )
        Text(
            text = tab.title,
            style = typography.body,
            color = if (active) colors.text else colors.textTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = dimens.tabTitleMaxWidth),
        )
        if (hovered || active) {
            Box(
                modifier =
                    Modifier
                        .size(dimens.tabCloseSize)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = onClose)
                        .pointerHoverIcon(PointerIcon.Hand),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AppIcons.Close,
                    contentDescription = "关闭标签页",
                    tint = colors.textTertiary,
                    modifier = Modifier.size(dimens.iconXs),
                )
            }
        }
    }
}

@Composable
private fun AddTabButton(onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val shape = RoundedCornerShape(dimens.radiusXs)
    Box(
        modifier =
            Modifier
                .padding(start = 2.dp)
                .size(dimens.tabAddSize)
                .clip(shape)
                .drawBehind {
                    drawRoundRect(
                        color = colors.borderStrong,
                        cornerRadius = CornerRadius(dimens.radiusXs.toPx()),
                        style =
                            Stroke(
                                width = 1.dp.toPx(),
                                pathEffect =
                                    PathEffect.dashPathEffect(
                                        floatArrayOf(4.dp.toPx(), 4.dp.toPx()),
                                    ),
                            ),
                    )
                }
                .clickable(onClick = onClick)
                .pointerHoverIcon(PointerIcon.Hand),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = AppIcons.Add,
            contentDescription = "新建标签页",
            tint = colors.textTertiary,
            modifier = Modifier.size(dimens.iconSm),
        )
    }
}

@Composable
private fun DatabasePill(
    state: AppState,
    enabled: Boolean,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val session = state.activeSession()
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(dimens.radiusControlSmall)
    Box {
        Row(
            modifier =
                Modifier
                    .height(dimens.dbPillHeight)
                    .background(colors.surface2, shape)
                    .border(1.dp, colors.border, shape)
                    .clickable(enabled = enabled) { expanded = !expanded }
                    .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
                    .padding(horizontal = dimens.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs + 1.dp),
        ) {
            Icon(
                imageVector = AppIcons.Layers,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(dimens.iconXs),
            )
            Text(
                text = "db${session?.currentDatabase ?: 0}",
                style = typography.monoValue,
                color = if (enabled) colors.textSecondary else colors.textQuaternary,
            )
            Icon(
                imageVector = AppIcons.ChevronDown,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(dimens.iconXs),
            )
        }
        AppMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            items =
                session?.databases.orEmpty().map { database ->
                    AppMenuItem(
                        label = "db${database.index}",
                        icon = AppIcons.Layers,
                        shortcut = formatCount(database.keyCount),
                        onClick = {
                            session?.let { state.selectDatabase(it.profile.id, database.index) }
                        },
                    )
                },
        )
    }
}

@Composable
private fun MoreMenu(state: AppState) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        AppIconButton(
            icon = AppIcons.More,
            contentDescription = "更多操作",
            onClick = { expanded = !expanded },
        )
        AppMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            items =
                listOf(
                    AppMenuItem(
                        label = "刷新",
                        icon = AppIcons.Refresh,
                        onClick = { state.showToast("刷新将在 Phase 4 接入") },
                    ),
                    AppMenuItem(
                        label = "关闭当前标签页",
                        icon = AppIcons.Close,
                        separatorBefore = true,
                        onClick = { state.activeTabId?.let(state::closeTab) },
                    ),
                ),
        )
    }
}

@Preview(widthDp = 1000, heightDp = 46)
@Composable
private fun WorkspaceTabBarPreview() {
    AppTheme(dark = true) {
        WorkspaceTabBar(state = previewStateWithConnections(openFirst = true))
    }
}
