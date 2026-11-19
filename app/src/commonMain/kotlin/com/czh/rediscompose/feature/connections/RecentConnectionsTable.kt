package com.czh.rediscompose.feature.connections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.component.AppCard
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.domain.connection.ConnectionProfile

@Composable
fun RecentConnectionsTable(
    connections: List<ConnectionProfile>,
    runtimeFor: (String) -> ConnectionRuntime,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    AppCard(
        modifier = modifier.fillMaxWidth(),
        title = "最近连接",
        description = "按最近使用时间排序 · 最多 6 条",
        contentPadding = 0.dp,
    ) {
        if (connections.isEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.spaceLg, vertical = dimens.spaceXl),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
            ) {
                Icon(
                    imageVector = AppIcons.Clock,
                    contentDescription = null,
                    tint = colors.textQuaternary,
                    modifier = Modifier.size(dimens.iconSm),
                )
                Text(
                    text = "还没有连接记录",
                    style = typography.caption,
                    color = colors.textTertiary,
                )
            }
        } else {
            TableHeader()
            connections.forEach { profile ->
                RecentRow(
                    profile = profile,
                    runtime = runtimeFor(profile.id),
                    onOpen = { onOpen(profile.id) },
                )
            }
        }
    }
}

@Composable
private fun TableHeader() {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(dimens.collectionHeaderHeight)
                .background(colors.surface2)
                .padding(horizontal = dimens.spaceLg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderCell("连接", 1.6f, TextAlign.Start, typography.tableHeader, colors.textQuaternary)
        HeaderCell("环境", 0.8f, TextAlign.Start, typography.tableHeader, colors.textQuaternary)
        HeaderCell("最后使用", 0.9f, TextAlign.Start, typography.tableHeader, colors.textQuaternary)
        HeaderCell("状态 / 延迟", 0.8f, TextAlign.End, typography.tableHeader, colors.textQuaternary)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
}

@Composable
private fun RowScope.HeaderCell(
    text: String,
    weight: Float,
    align: TextAlign,
    style: TextStyle,
    color: Color,
) {
    Text(
        text = text,
        style = style,
        color = color,
        textAlign = align,
        maxLines = 1,
        modifier = Modifier.weight(weight),
    )
}

@Composable
private fun RecentRow(
    profile: ConnectionProfile,
    runtime: ConnectionRuntime,
    onOpen: () -> Unit,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(dimens.recentTableRowHeight + 6.dp)
                .background(if (hovered) colors.surface2 else colors.surface)
                .hoverable(interaction)
                .clickable(onClick = onOpen)
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(horizontal = dimens.spaceLg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1.6f)) {
            Text(
                text = profile.name,
                style = typography.bodySmall,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${profile.host}:${profile.port}",
                style = typography.monoMicro,
                color = colors.textQuaternary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = profile.environment.displayName(),
            style = typography.tooltip,
            color = colors.textTertiary,
            maxLines = 1,
            modifier = Modifier.weight(0.8f),
        )
        Text(
            text = relativeConnectionTime(profile.lastOpenedAtEpochMillis),
            style = typography.monoMicro,
            color = colors.textTertiary,
            maxLines = 1,
            modifier = Modifier.weight(0.9f),
        )
        Box(Modifier.weight(0.8f), contentAlignment = Alignment.CenterEnd) {
            ConnectionStatePill(runtime = runtime)
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.spaceLg)
            .height(1.dp)
            .background(colors.border),
    )
}
