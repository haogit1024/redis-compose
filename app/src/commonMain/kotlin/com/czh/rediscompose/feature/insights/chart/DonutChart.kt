package com.czh.rediscompose.feature.insights.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens

/**
 * Simple used-vs-max donut.
 *
 * The caller only renders it when a real `maxmemory` limit exists, so a zero/unknown
 * denominator is never shown as a percentage.
 */
@Composable
fun DonutChart(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color? = null,
    trackColor: Color? = null,
    strokeWidth: Dp = LocalAppDimens.current.donutStroke,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val arcColor = color ?: colors.accent
    val track = trackColor ?: colors.surface3
    val animated = remember { Animatable(0f) }

    LaunchedEffect(progress) {
        animated.animateTo(
            targetValue = progress.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        )
    }

    Box(
        modifier = modifier.size(dimens.donutSize),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke),
            )
            if (animated.value > 0f) {
                drawArc(
                    color = arcColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animated.value,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style =
                        Stroke(
                            width = stroke,
                            cap = StrokeCap.Round,
                        ),
                )
            }
        }
        content()
    }
}
