package com.czh.rediscompose.feature.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppColors
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography

@Composable
fun HealthCard(
    items: List<HealthItem>,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val overallTone =
        when {
            items.any { it.tone == HealthTone.DANGER } -> HealthTone.DANGER
            items.any { it.tone == HealthTone.WARN } -> HealthTone.WARN
            items.any { it.tone == HealthTone.UNKNOWN } -> HealthTone.UNKNOWN
            else -> HealthTone.OK
        }
    InsightsCard(
        title = "健康检查",
        description = "REPLICATION & PERSISTENCE",
        modifier = modifier,
        actions = {
            Box(
                Modifier
                    .size(dimens.tinyDotSize)
                    .background(overallTone.color(colors), CircleShape),
            )
        },
    ) {
        Column(
            modifier =
                Modifier.padding(
                    start = dimens.chartCardPaddingH,
                    end = dimens.chartCardPaddingH,
                    top = 2.dp,
                    bottom = dimens.spaceMd,
                ),
        ) {
            items.forEachIndexed { index, item ->
                HealthRow(item)
                if (index != items.lastIndex) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = dimens.spaceXs)
                            .height(1.dp)
                            .background(colors.border),
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthRow(item: HealthItem) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val toneColor = item.tone.color(colors)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = dimens.spaceXs + 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
    ) {
        Icon(
            imageVector = item.tone.icon(),
            contentDescription = null,
            tint = toneColor,
            modifier = Modifier.size(dimens.iconSm),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = typography.healthText,
                color = colors.text,
                maxLines = 1,
            )
            Text(
                text = item.detail,
                style = typography.healthDetail,
                color = colors.textQuaternary,
                maxLines = 1,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        Text(
            text = item.label,
            style = typography.monoMicro,
            color = toneColor,
            maxLines = 1,
        )
    }
}

private fun HealthTone.color(colors: AppColors): Color =
    when (this) {
        HealthTone.OK -> colors.success
        HealthTone.WARN -> colors.warning
        HealthTone.DANGER -> colors.danger
        HealthTone.UNKNOWN -> colors.textTertiary
    }

private fun HealthTone.icon(): ImageVector =
    when (this) {
        HealthTone.OK -> AppIcons.CheckCircle
        HealthTone.WARN -> AppIcons.Warning
        HealthTone.DANGER -> AppIcons.ErrorCircle
        HealthTone.UNKNOWN -> AppIcons.Info
    }
