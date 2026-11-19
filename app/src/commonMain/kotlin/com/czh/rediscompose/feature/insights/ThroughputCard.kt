package com.czh.rediscompose.feature.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.feature.insights.chart.AreaChart
import com.czh.rediscompose.feature.insights.chart.ChartSeriesData

@Composable
fun ThroughputCard(
    data: ThroughputData,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val hasSamples = data.total.size >= 2 && data.labels.isNotEmpty()
    InsightsCard(
        title = "吞吐量",
        description = "自本次连接 · 5s 采样",
        modifier = modifier,
        actions = {
            LegendDot(color = colors.accent, label = "命令")
            LegendDot(color = colors.success, label = "读取")
        },
    ) {
        if (!hasSamples) {
            InsightsEmptyHint(
                text = "正在收集数据…",
                description = "保持页面打开以收集样本",
                icon = AppIcons.Timeline,
                modifier = Modifier.padding(horizontal = dimens.spaceSm),
            )
        } else {
            Column(
                modifier =
                    Modifier.padding(
                        start = dimens.spaceSm + 2.dp,
                        end = dimens.spaceSm + 2.dp,
                        top = 2.dp,
                        bottom = dimens.spaceMd,
                    ),
            ) {
                AreaChart(
                    labels = data.labels,
                    series =
                        listOf(
                            ChartSeriesData(
                                name = "命令",
                                values = data.total,
                                color = colors.accent,
                                filled = true,
                            ),
                            ChartSeriesData(
                                name = "读取",
                                values = data.reads,
                                color = colors.success,
                                filled = false,
                            ),
                        ),
                )
            }
        }
    }
}

@Composable
private fun LegendDot(
    color: Color,
    label: String,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs),
    ) {
        Box(
            Modifier
                .size(6.dp)
                .background(color, RoundedCornerShape(2.dp)),
        )
        Text(
            text = label,
            style = typography.monoMicro,
            color = colors.textTertiary,
        )
    }
}
