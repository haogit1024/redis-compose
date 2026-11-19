package com.czh.rediscompose.feature.insights.chart

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path

/**
 * Geometry helpers shared by the insights charts.
 *
 * Lines use the same mid-point cubic Bézier smoothing as the HTML design; a chart with
 * fewer than two points never produces a path.
 */
object ChartPrimitives {
    /** Mid-point cubic smoothing: for each pair, both controls sit on the horizontal midpoint. */
    fun smoothPath(points: List<Offset>): Path {
        val path = Path()
        if (points.size < 2) return path
        path.moveTo(points.first().x, points.first().y)
        for (index in 1 until points.size) {
            val previous = points[index - 1]
            val current = points[index]
            val midX = (previous.x + current.x) / 2f
            path.cubicTo(midX, previous.y, midX, current.y, current.x, current.y)
        }
        return path
    }

    /** The same smoothing, closed along [baselineY] for a filled area. */
    fun smoothAreaPath(
        points: List<Offset>,
        baselineY: Float,
    ): Path {
        val path = smoothPath(points)
        if (points.isEmpty()) return path
        path.lineTo(points.last().x, baselineY)
        path.lineTo(points.first().x, baselineY)
        path.close()
        return path
    }

    /** Maps values into a top-left origin canvas of [width] × [height]. */
    fun chartPoints(
        values: List<Float>,
        width: Float,
        height: Float,
        maxValue: Double,
    ): List<Offset> {
        if (values.size < 2 || width <= 0f || height <= 0f) return emptyList()
        val safeMax = maxValue.takeIf { it.isFinite() && it > 0.0 } ?: 1.0
        val step = width / (values.size - 1).toFloat()
        return values.mapIndexed { index, value ->
            val normalized = (value / safeMax).coerceIn(0.0, 1.0).toFloat()
            Offset(x = index * step, y = height - normalized * height)
        }
    }

    /** Three horizontal grid ticks: max, half and zero. */
    fun gridTicks(maxValue: Double): List<Double> = listOf(maxValue, maxValue / 2.0, 0.0)
}
