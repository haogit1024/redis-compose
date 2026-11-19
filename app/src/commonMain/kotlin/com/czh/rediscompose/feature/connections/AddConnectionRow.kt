package com.czh.rediscompose.feature.connections

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.component.AppBadge
import com.czh.rediscompose.design.component.AppBadgeTone
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddConnectionRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(dimens.connectionCardRadius)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 82.dp)
                .drawBehind {
                    val stroke = 1.dp.toPx()
                    drawRoundRect(
                        color = if (hovered) colors.borderEmphasis else colors.borderStrong,
                        cornerRadius = CornerRadius(dimens.connectionCardRadius.toPx()),
                        style =
                            Stroke(
                                width = stroke,
                                pathEffect =
                                    PathEffect.dashPathEffect(
                                        floatArrayOf(5.dp.toPx(), 4.dp.toPx()),
                                    ),
                            ),
                    )
                }
                .background(if (hovered) colors.surface2 else androidx.compose.ui.graphics.Color.Transparent, shape)
                .hoverable(interaction)
                .clickable(onClick = onClick)
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(horizontal = 18.dp, vertical = dimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceMd),
    ) {
        Box(
            modifier =
                Modifier
                    .size(34.dp)
                    .background(colors.accentSoft, RoundedCornerShape(dimens.connectionLogoRadius)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.Add,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(dimens.iconSm),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = "新建连接",
                style = typography.bodyStrong,
                color = if (hovered) colors.text else colors.textSecondary,
            )
            Text(
                text = "配置 Redis 实例、TLS 与 SSH 隧道",
                style = typography.caption,
                color = colors.textQuaternary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs)) {
            AppBadge("TLS", tone = AppBadgeTone.Plain, icon = AppIcons.Shield)
            AppBadge("SSH", tone = AppBadgeTone.Plain, icon = AppIcons.Link)
        }
    }
}
