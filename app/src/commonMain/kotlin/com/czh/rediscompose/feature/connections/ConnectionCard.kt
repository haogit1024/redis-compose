package com.czh.rediscompose.feature.connections

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.app.SessionStatus
import com.czh.rediscompose.design.component.AppBadge
import com.czh.rediscompose.design.component.AppBadgeTone
import com.czh.rediscompose.design.component.AppIconButton
import com.czh.rediscompose.design.component.AppIconButtonSize
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.domain.connection.ConnectionProfile
import com.czh.rediscompose.util.formatCount

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConnectionCard(
    profile: ConnectionProfile,
    runtime: ConnectionRuntime,
    modifier: Modifier = Modifier,
    testing: Boolean = false,
    onOpen: () -> Unit = {},
    onEdit: () -> Unit = {},
    onDuplicate: () -> Unit = {},
    onTest: () -> Unit = {},
    onDelete: () -> Unit = {},
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val connectionColor = profile.color.toColor(colors)
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val borderColor by animateColorAsState(
        targetValue = if (hovered) colors.borderEmphasis else colors.border,
        animationSpec = tween(durationMillis = 140),
        label = "connectionCardBorder",
    )
    var menuExpanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(dimens.connectionCardRadius)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(if (hovered) colors.surfaceHover else colors.surface, shape)
                .border(1.dp, borderColor, shape)
                .hoverable(interaction)
                .clickable(onClick = onOpen)
                .pointerHoverIcon(PointerIcon.Hand)
                .drawBehind {
                    val lineHeight = dimens.connectionCardTopLine.toPx()
                    drawRect(
                        brush =
                            Brush.horizontalGradient(
                                colors = listOf(connectionColor, connectionColor.copy(alpha = 0f)),
                                endX = size.width * 0.82f,
                            ),
                        topLeft = Offset.Zero,
                        size = androidx.compose.ui.geometry.Size(size.width, lineHeight),
                    )
                }
                .padding(dimens.connectionCardPadding),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceMd),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(dimens.connectionLogoSize)
                        .background(connectionColor.copy(alpha = 0.11f), RoundedCornerShape(dimens.connectionLogoRadius))
                        .border(
                            1.dp,
                            connectionColor.copy(alpha = 0.22f),
                            RoundedCornerShape(dimens.connectionLogoRadius),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = profile.initials(),
                    style = typography.monoValue,
                    color = connectionColor,
                    maxLines = 1,
                )
            }
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs + 2.dp),
                ) {
                    Text(
                        text = profile.name,
                        style = typography.bodyStrong,
                        color = colors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    TinyEnvironmentBadge(profile.environment.displayName())
                }
                Text(
                    text = "${profile.host}:${profile.port}",
                    style = typography.monoMeta,
                    color = colors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            ConnectionStatePill(runtime = runtime)
            AppIconButton(
                icon = AppIcons.More,
                contentDescription = "连接操作",
                onClick = { menuExpanded = !menuExpanded },
                size = AppIconButtonSize.Small,
            )
            ConnectionCardMenu(
                expanded = menuExpanded,
                testing = testing,
                onDismiss = { menuExpanded = false },
                onOpen = onOpen,
                onEdit = onEdit,
                onDuplicate = onDuplicate,
                onTest = onTest,
                onDelete = onDelete,
            )
        }

        FlowRow(
            modifier = Modifier.padding(top = dimens.spaceMd),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs + 1.dp),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceXs + 1.dp),
        ) {
            if (profile.tls.enabled) AppBadge("TLS", tone = AppBadgeTone.Plain, icon = AppIcons.Shield)
            if (profile.ssh.enabled) AppBadge("SSH 隧道", tone = AppBadgeTone.Plain, icon = AppIcons.Link)
            if (profile.readOnly) AppBadge("只读", tone = AppBadgeTone.Warning, icon = AppIcons.Shield)
            runtime.version?.takeIf { it.isNotBlank() }?.let { AppBadge("Redis $it", tone = AppBadgeTone.Plain) }
            runtime.databaseCount?.let { AppBadge("$it DB", tone = AppBadgeTone.Plain) }
            if (profile.defaultDatabase != 0) AppBadge("db${profile.defaultDatabase}", tone = AppBadgeTone.Plain)
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = dimens.spaceSm + 2.dp)
                    .drawBehind {
                        drawLine(
                            color = colors.border,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                    .padding(top = dimens.spaceSm + 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceMd),
        ) {
            Stat(icon = AppIcons.Layers, text = runtime.totalKeys?.let { "${formatCount(it)} keys" } ?: "— keys")
            if (runtime.status == SessionStatus.ERROR && runtime.errorMessage != null) {
                Stat(
                    icon = AppIcons.Warning,
                    text = runtime.errorMessage.lineSequence().firstOrNull() ?: "连接失败",
                    color = colors.danger,
                )
            } else {
                Stat(icon = AppIcons.Clients, text = runtime.connectedClients?.let { "$it clients" } ?: "— clients")
            }
            Text(
                text = relativeConnectionTime(profile.lastOpenedAtEpochMillis),
                style = typography.monoMicro,
                color = colors.textTertiary,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun ConnectionStatePill(runtime: ConnectionRuntime) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val color = runtime.stateColor(colors)
    val shape = RoundedCornerShape(dimens.radiusChip)
    Row(
        modifier =
            Modifier
                .height(dimens.connectionStateHeight)
                .background(color.copy(alpha = 0.12f), shape)
                .border(1.dp, color.copy(alpha = 0.22f), shape)
                .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier
                .size(4.dp)
                .background(color, CircleShape),
        )
        Text(
            text = runtime.stateLabel(),
            style = typography.monoMicro,
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
private fun TinyEnvironmentBadge(text: String) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val shape = RoundedCornerShape(dimens.radiusChip)
    Box(
        modifier =
            Modifier
                .height(17.dp)
                .background(colors.surface3, shape)
                .border(1.dp, colors.border, shape)
                .padding(horizontal = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = typography.monoMicro,
            color = colors.textTertiary,
            maxLines = 1,
        )
    }
}

@Composable
private fun Stat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color = LocalAppColors.current.textQuaternary,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(10.dp),
        )
        Text(
            text = text,
            style = typography.monoMicro,
            color = if (color == colors.textQuaternary) colors.textQuaternary else color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
