package com.czh.rediscompose.design.icon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors

/**
 * Self-drawn brand mark: a gradient tile with three stacked layers.
 * No image asset is required, so it stays crisp at any density.
 */
@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 27.dp,
) {
    Box(
        modifier =
            modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.3f))
                .background(
                    Brush.linearGradient(
                        colors =
                            listOf(
                                Color(0xFFA49BFF),
                                Color(0xFF6C67F2),
                                Color(0xFF27D3B4),
                            ),
                        start = Offset.Zero,
                        end = Offset.Infinite,
                    ),
                ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val unit = this.size.minDimension
            val stroke = Stroke(width = unit * 0.075f)
            val corner = CornerRadius(unit * 0.05f, unit * 0.05f)
            val widths = listOf(0.56f, 0.44f, 0.32f)
            widths.forEachIndexed { index, widthRatio ->
                val barWidth = unit * widthRatio
                val barHeight = unit * 0.115f
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.92f - index * 0.22f),
                    topLeft =
                        Offset(
                            x = (unit - barWidth) / 2f,
                            y = unit * (0.30f + index * 0.18f),
                        ),
                    size = Size(barWidth, barHeight),
                    cornerRadius = corner,
                    style = stroke,
                )
            }
        }
    }
}

@Preview
@Composable
private fun BrandMarkPreview() {
    AppTheme(dark = true) {
        Box(Modifier.background(LocalAppColors.current.background)) {
            BrandMark()
        }
    }
}
