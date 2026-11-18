package com.czh.rediscompose.feature.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.app.AppPage
import com.czh.rediscompose.app.AppState
import com.czh.rediscompose.app.WorkspaceTabKind
import com.czh.rediscompose.design.component.AppTooltip
import com.czh.rediscompose.design.component.ToastKind
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens

@Composable
fun AppRail(
    state: AppState,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val tabKind = state.activeTab()?.kind
    val onWorkspace = state.activePage == AppPage.WORKSPACE
    val connected = state.sessions.values.any { it.isConnected }

    Column(
        modifier =
            modifier
                .width(dimens.railWidth)
                .fillMaxHeight()
                .background(colors.surface)
                .drawBehind {
                    drawRect(
                        color = colors.border,
                        topLeft = Offset(size.width - 1.dp.toPx(), 0f),
                        size = Size(1.dp.toPx(), size.height),
                    )
                }
                .padding(vertical = dimens.spaceMd),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
    ) {
        RailButton(
            icon = AppIcons.Key,
            contentDescription = "键浏览器",
            active = onWorkspace && tabKind != WorkspaceTabKind.CONSOLE && tabKind != WorkspaceTabKind.INSIGHTS,
            onClick = { openWorkspaceTab(state, WorkspaceTabKind.KEYS) },
        )
        RailButton(
            icon = AppIcons.Console,
            contentDescription = "命令控制台",
            active = onWorkspace && tabKind == WorkspaceTabKind.CONSOLE,
            onClick = { openWorkspaceTab(state, WorkspaceTabKind.CONSOLE) },
        )
        RailButton(
            icon = AppIcons.Insights,
            contentDescription = "服务器洞察",
            active = onWorkspace && tabKind == WorkspaceTabKind.INSIGHTS,
            onClick = { openWorkspaceTab(state, WorkspaceTabKind.INSIGHTS) },
        )
        Box(
            Modifier
                .padding(vertical = dimens.spaceXs)
                .width(dimens.railSeparatorWidth)
                .height(1.dp)
                .background(colors.border),
        )
        RailButton(
            icon = AppIcons.Connections,
            contentDescription = "连接管理",
            active = state.activePage == AppPage.CONNECTIONS,
            onClick = { state.setPage(AppPage.CONNECTIONS) },
        )
        RailButton(
            icon = AppIcons.Settings,
            contentDescription = "偏好设置",
            active = state.activePage == AppPage.SETTINGS,
            onClick = { state.setPage(AppPage.SETTINGS) },
        )
        Spacer(Modifier.weight(1f))
        if (connected) {
            Box(
                Modifier
                    .padding(bottom = dimens.spaceSm)
                    .size(dimens.dotSize)
                    .drawBehind {
                        drawCircle(
                            color = colors.success.copy(alpha = 0.12f),
                            radius = size.minDimension / 2f + 3.dp.toPx(),
                        )
                    }
                    .background(colors.success, CircleShape),
            )
        }
        AppTooltip("快捷键") {
            RailButton(
                icon = AppIcons.Keyboard,
                contentDescription = "快捷键",
                active = false,
                onClick = { state.showToast("快捷键将在 Phase 9 提供", ToastKind.Info) },
            )
        }
    }
}

@Composable
private fun RailButton(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(dimens.railButtonRadius)
    val background =
        when {
            active -> colors.accentSoft
            hovered -> colors.surface2
            else -> Color.Transparent
        }
    val borderColor =
        when {
            active -> colors.accent.copy(alpha = 0.24f)
            hovered -> colors.border
            else -> Color.Transparent
        }
    AppTooltip(contentDescription) {
        Box(
            modifier =
                Modifier
                    .size(dimens.railButtonSize)
                    .drawBehind {
                        if (active) {
                            drawRoundRect(
                                color = colors.accent,
                                topLeft =
                                    Offset(
                                        x = -((dimens.railWidth - dimens.railButtonSize) / 2).toPx(),
                                        y = ((dimens.railButtonSize - dimens.railActiveBarHeight) / 2).toPx(),
                                    ),
                                size =
                                    Size(
                                        dimens.railActiveBarWidth.toPx(),
                                        dimens.railActiveBarHeight.toPx(),
                                    ),
                                cornerRadius = CornerRadius(dimens.railActiveBarWidth.toPx() / 2f),
                            )
                        }
                    }
                    .clip(shape)
                    .background(background)
                    .border(1.dp, borderColor, shape)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onClick,
                    )
                    .pointerHoverIcon(PointerIcon.Hand),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint =
                    if (active) {
                        colors.accent
                    } else if (hovered) {
                        colors.text
                    } else {
                        colors.textTertiary
                    },
                modifier = Modifier.size(dimens.railIconSize),
            )
        }
    }
}

private fun openWorkspaceTab(
    state: AppState,
    kind: WorkspaceTabKind,
) {
    val connectionId = state.activeConnectionId
    if (connectionId == null) {
        state.setPage(AppPage.CONNECTIONS)
        state.showToast("请先选择一个连接", ToastKind.Warning)
        return
    }
    state.openTab(connectionId, kind)
}

@Preview(widthDp = 60, heightDp = 720)
@Composable
private fun AppRailPreview() {
    AppTheme(dark = true) {
        AppRail(state = previewEmptyState())
    }
}
