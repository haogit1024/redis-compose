package com.czh.rediscompose.feature.insights.chart

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.theme.LocalAppColors

/**
 * Tiny trend line for KPI cards.
 *
 * It never draws a curve from a single point; with fewer than two samples the area stays
 * empty instead of implying history.
 */
@Composable
fun Sparkline(
    values: List<Float>,
    modifier: Modifier = Modifier,
    color: Color? = null,
    filled: Boolean = true,
) {
    val accent = color ?: LocalAppColors.current.accent
    Box(
        modifier =
            modifier.drawWithCache {
                val points =
                    if (values.size < 2 || size.width <= 0f || size.height <= 0f) {
                        emptyList()
                    } else {
                        val minimum = values.min()
                        val maximum = values.max()
                        val span = maximum - minimum
                        val usableHeight = size.height * 0.78f
                        val topInset = size.height * 0.12f
                        values.mapIndexed { index, value ->
                            val normalized = if (span > 0f) (value - minimum) / span else 0.5f
                            Offset(
                                x = index * size.width / (values.size - 1).toFloat(),
                                y = size.height - topInset - normalized * usableHeight,
                            )
                        }
                    }
                val line = ChartPrimitives.smoothPath(points)
                val area = ChartPrimitives.smoothAreaPath(points, size.height)
                val gradient =
                    Brush.verticalGradient(
                        colors = listOf(accent.copy(alpha = 0.20f), accent.copy(alpha = 0f)),
                        startY = 0f,
                        endY = size.height,
                    )
                onDrawBehind {
                    if (points.size < 2) return@onDrawBehind
                    if (filled) drawPath(area, brush = gradient)
                    drawPath(
                        path = line,
                        color = accent,
                        style =
                            Stroke(
                                width = 1.4.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                    )
                }
            },
    )
}
