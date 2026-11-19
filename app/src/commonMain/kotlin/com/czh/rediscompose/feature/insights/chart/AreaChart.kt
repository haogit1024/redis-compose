package com.czh.rediscompose.feature.insights.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.feature.insights.InsightsMetrics

data class ChartSeriesData(
    val name: String,
    val values: List<Float>,
    val color: Color,
    val filled: Boolean = false,
)

/**
 * Self-drawn area/line chart: dashed grid, compact axis labels, smoothed paths and a
 * 900ms reveal. Values are drawn only when at least two points exist.
 */
@Composable
fun AreaChart(
    labels: List<String>,
    series: List<ChartSeriesData>,
    modifier: Modifier = Modifier,
    height: Dp = LocalAppDimens.current.chartHeight,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val maxValue = series.maxOfOrNull { entry -> entry.values.maxOrNull() ?: 0f }?.toDouble() ?: 0.0
    val chartMax = InsightsMetrics.niceCeil(maxValue)
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        )
    }

    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(height)) {
            Column(
                modifier =
                    Modifier
                        .width(dimens.chartAxisWidth)
                        .height(height)
                        .padding(end = dimens.spaceXs),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                Text(InsightsMetrics.formatCompact(chartMax), style = typography.chartAxis, color = colors.textQuaternary)
                Text(InsightsMetrics.formatCompact(chartMax / 2.0), style = typography.chartAxis, color = colors.textQuaternary)
                Text("0", style = typography.chartAxis, color = colors.textQuaternary)
            }
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(height)
                        .drawWithCache {
                            val chartWidth = size.width
                            val chartHeight = size.height
                            val paths =
                                series.map { entry ->
                                    val points =
                                        ChartPrimitives.chartPoints(
                                            values = entry.values,
                                            width = chartWidth,
                                            height = chartHeight,
                                            maxValue = chartMax,
                                        )
                                    Triple(
                                        entry,
                                        ChartPrimitives.smoothPath(points),
                                        ChartPrimitives.smoothAreaPath(points, chartHeight),
                                    )
                                }
                            val gridEffect =
                                androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                    floatArrayOf(4.dp.toPx(), 4.dp.toPx()),
                                )
                            onDrawBehind {
                                ChartPrimitives.gridTicks(chartMax).forEachIndexed { index, tick ->
                                    val y = chartHeight - (tick / chartMax).toFloat() * chartHeight
                                    drawLine(
                                        color = colors.border,
                                        start = Offset(0f, y.coerceIn(0f, chartHeight)),
                                        end = Offset(chartWidth, y.coerceIn(0f, chartHeight)),
                                        strokeWidth = 1.dp.toPx(),
                                        pathEffect = gridEffect,
                                    )
                                }
                                clipRect(right = chartWidth * progress.value) {
                                    paths.forEach { (entry, line, area) ->
                                        if (line.isEmpty) return@forEach
                                        if (entry.filled) {
                                            drawPath(
                                                path = area,
                                                brush =
                                                    Brush.verticalGradient(
                                                        colors =
                                                            listOf(
                                                                entry.color.copy(alpha = 0.34f),
                                                                entry.color.copy(alpha = 0f),
                                                            ),
                                                        startY = 0f,
                                                        endY = chartHeight,
                                                    ),
                                            )
                                        }
                                        drawPath(
                                            path = line,
                                            color = entry.color,
                                            style =
                                                Stroke(
                                                    width = 2.dp.toPx(),
                                                    cap = StrokeCap.Round,
                                                    join = StrokeJoin.Round,
                                                ),
                                        )
                                    }
                                }
                            }
                        },
            )
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = dimens.chartAxisWidth, top = dimens.spaceXs),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            labels.forEach { label ->
                Text(
                    text = label,
                    style = typography.chartAxis,
                    color = colors.textQuaternary,
                    maxLines = 1,
                )
            }
        }
    }
}
