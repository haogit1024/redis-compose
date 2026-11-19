package com.czh.rediscompose.feature.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.component.AppBadge
import com.czh.rediscompose.design.component.AppBadgeTone
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.util.formatCount

@Composable
fun ClientsCard(
    summary: ClientsSummary,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val maxGroup = summary.groups.maxOfOrNull { it.count } ?: 0
    InsightsCard(
        title = "客户端",
        description = "CLIENT LIST · ${summary.connectedClients ?: summary.groups.sumOf { it.count }} connected",
        modifier = modifier,
        actions = {
            AppBadge(
                text = if (summary.listAvailable) "已读取" else "不可用",
                tone = if (summary.listAvailable) AppBadgeTone.Success else AppBadgeTone.Warning,
                icon = AppIcons.Clients,
            )
        },
    ) {
        Column(
            modifier =
                Modifier.padding(
                    start = dimens.chartCardPaddingH,
                    end = dimens.chartCardPaddingH,
                    top = dimens.spaceXs,
                    bottom = dimens.chartCardPaddingH,
                ),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceSm),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
                ClientStat("应用连接", summary.appConnections.toString(), Modifier.weight(1f))
                ClientStat("管理会话", summary.managementSessions.toString(), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
                ClientStat("空闲", summary.idleConnections.toString(), Modifier.weight(1f))
                ClientStat("最大连接", summary.maxClients?.let { formatCount(it.toLong()) } ?: "—", Modifier.weight(1f))
            }
            if (!summary.listAvailable) {
                InsightsEmptyHint(
                    text = "CLIENT LIST 未返回客户端",
                    description = "${summary.connectedClients ?: 0} 个连接仍保持在线",
                    icon = AppIcons.Clients,
                )
            } else {
                Column(
                    modifier = Modifier.padding(top = dimens.spaceXs),
                    verticalArrangement = Arrangement.spacedBy(dimens.spaceSm),
                ) {
                    summary.groups.forEach { group ->
                        val color =
                            when {
                                group.name == "other" -> colors.warning
                                group.name == summary.groups.first().name -> colors.accent
                                else -> colors.success
                            }
                        ClientGroupRow(
                            name = group.name,
                            count = group.count,
                            fraction = if (maxGroup > 0) group.count.toFloat() / maxGroup.toFloat() else 0f,
                            color = color,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    Column(
        modifier =
            modifier
                .background(colors.surface2, RoundedCornerShape(dimens.radiusControlSmall))
                .padding(horizontal = dimens.spaceSm + 1.dp, vertical = dimens.spaceSm),
    ) {
        Text(
            text = label,
            style = typography.tooltip,
            color = colors.textQuaternary,
            maxLines = 1,
        )
        Text(
            text = value,
            style = typography.monoValue,
            color = colors.text,
            maxLines = 1,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun ClientGroupRow(
    name: String,
    count: Int,
    fraction: Float,
    color: Color,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm + 1.dp),
    ) {
        Text(
            text = name,
            style = typography.monoMeta,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(dimens.commandNameWidth),
        )
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .height(dimens.barTrackHeight)
                    .background(colors.surface3, RoundedCornerShape(5.dp)),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .background(color, RoundedCornerShape(5.dp)),
            )
        }
        Text(
            text = count.toString(),
            style = typography.monoMicro,
            color = colors.textTertiary,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(dimens.commandValueWidth),
        )
    }
}
