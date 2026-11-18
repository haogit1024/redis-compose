package com.czh.rediscompose.design.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens

/**
 * Thin custom scrollbar matching the design tokens.
 *
 * Drawn in `commonMain` (instead of the desktop-only `VerticalScrollbar`) so the same
 * component works when the app grows additional targets.
 */
@Composable
fun AppVerticalScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val thumbColor = if (hovered) colors.borderEmphasis else colors.borderStrong
    val minThumbHeight = 32.dp

    Box(
        modifier =
            modifier
                .width(dimens.scrollbarWidth)
                .fillMaxHeight()
                .hoverable(interactionSource)
                .pointerHoverIcon(PointerIcon.Default)
                .pointerInput(scrollState.maxValue, scrollState.viewportSize) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val max = scrollState.maxValue
                        val viewport = scrollState.viewportSize
                        if (max > 0 && viewport > 0) {
                            val track = size.height.toFloat()
                            val thumb = thumbHeight(track, viewport, max, minThumbHeight.toPx())
                            val scrollable = (track - thumb).coerceAtLeast(1f)
                            scrollState.dispatchRawDelta(dragAmount.y / scrollable * max)
                        }
                    }
                },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val max = scrollState.maxValue
            if (max <= 0) return@Canvas
            val trackHeight = size.height
            val thumb = thumbHeight(trackHeight, scrollState.viewportSize, max, minThumbHeight.toPx())
            val top = (scrollState.value.toFloat() / max) * (trackHeight - thumb)
            drawRoundRect(
                color = thumbColor,
                topLeft = Offset(0f, top),
                size = Size(size.width, thumb),
                cornerRadius = CornerRadius(size.width / 2f),
            )
        }
    }
}

private fun thumbHeight(
    track: Float,
    viewport: Int,
    max: Int,
    minHeight: Float,
): Float {
    val content = max + viewport
    if (content <= 0) return track
    return (track * viewport / content).coerceIn(minHeight.coerceAtMost(track), track)
}

@Preview
@Composable
private fun AppVerticalScrollbarPreview() {
    AppTheme(dark = true) {
        val state = rememberScrollState()
        val colors = LocalAppColors.current
        Box(
            Modifier.size(width = 280.dp, height = 180.dp),
        ) {
            Row(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(state),
                ) {
                    repeat(20) {
                        Text(
                            text = "第 ${it + 1} 行",
                            color = colors.textSecondary,
                            modifier = Modifier.size(width = 220.dp, height = 24.dp),
                        )
                    }
                }
                AppVerticalScrollbar(state)
            }
        }
    }
}
