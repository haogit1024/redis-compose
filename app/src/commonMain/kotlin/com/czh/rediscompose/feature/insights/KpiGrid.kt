package com.czh.rediscompose.feature.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.feature.insights.chart.Sparkline

/**
 * Four KPI tiles, two per row on narrow windows.
 *
 * Sparklines come from the session samples; with fewer than two points the line stays
 * empty instead of drawing a fake trend.
 */
@Composable
fun KpiGrid(
    kpis: List<KpiData>,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalAppDimens.current
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 900.dp) 4 else 2
        Column(verticalArrangement = Arrangement.spacedBy(dimens.insightsGap)) {
            kpis.chunked(columns).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimens.insightsGap),
                ) {
                    rowItems.forEach { kpi ->
                        KpiCard(kpi = kpi, modifier = Modifier.weight(1f))
                    }
                    repeat(columns - rowItems.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    kpi: KpiData,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val shape = RoundedCornerShape(dimens.insightsCardRadius)
    Box(
        modifier =
            modifier
                .heightIn(min = dimens.kpiMinHeight)
                .clip(shape)
                .background(colors.surface, shape)
                .border(1.dp, colors.border, shape),
    ) {
        if (kpi.sparkline.size >= 2) {
            Sparkline(
                values = kpi.sparkline,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(width = dimens.kpiSparkWidth, height = dimens.kpiSparkHeight)
                        .graphicsLayer { alpha = 0.75f },
            )
        }
        Column(
            modifier =
                Modifier.padding(
                    horizontal = dimens.kpiPaddingH,
                    vertical = dimens.kpiPaddingV,
                ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    imageVector = kpi.icon,
                    contentDescription = null,
                    tint = colors.textQuaternary,
                    modifier = Modifier.size(dimens.iconXs),
                )
                Text(
                    text = kpi.title,
                    style = typography.kpiLabel,
                    color = colors.textTertiary,
                    maxLines = 1,
                )
            }
            Row(
                modifier = Modifier.padding(top = dimens.spaceSm),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs + 2.dp),
            ) {
                Text(
                    text = kpi.value,
                    style = typography.kpiValue,
                    color = colors.text,
                    maxLines = 1,
                )
                if (kpi.unit != null) {
                    Text(
                        text = kpi.unit,
                        style = typography.monoMicro,
                        color = colors.textQuaternary,
                        maxLines = 1,
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                }
            }
            if (kpi.delta != null) {
                Text(
                    text = kpi.delta,
                    style = typography.monoMicro,
                    color =
                        when (kpi.deltaTone) {
                            KpiDeltaTone.Neutral -> colors.textTertiary
                            KpiDeltaTone.Positive -> colors.success
                            KpiDeltaTone.Warning -> colors.warning
                        },
                    maxLines = 1,
                    modifier = Modifier.padding(top = dimens.spaceXs + 2.dp),
                )
            }
        }
    }
}
