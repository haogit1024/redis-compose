package com.czh.rediscompose.design.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

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

/**
 * Scrollbar for [LazyListState]-backed pages.
 *
 * Item heights vary, so the thumb is proportional to the visible item count and dragging
 * scrolls by an estimated pixel delta. It is intentionally lightweight: settings is the
 * only long form page, and the wheel/scroll gestures remain the primary input.
 */
@Composable
fun AppVerticalScrollbar(
    scrollState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val thumbColor = if (hovered) colors.borderEmphasis else colors.borderStrong
    val layout = scrollState.layoutInfo
    val total = layout.totalItemsCount
    val visibleItems = layout.visibleItemsInfo
    val visibleCount = visibleItems.size
    val canScroll = visibleCount > 0 && total > visibleCount
    val averageItem = if (visibleCount > 0) visibleItems.sumOf { it.size } / visibleCount else 1
    val first = visibleItems.firstOrNull()
    val scrolled = ((first?.index ?: 0) * averageItem + (first?.offset ?: 0)).toFloat()
    val totalPixels = (total * averageItem).coerceAtLeast(1).toFloat()
    val fraction = (scrolled / totalPixels).coerceIn(0f, 1f)
    val minThumbHeight = 32.dp

    Box(
        modifier =
            modifier
                .width(dimens.scrollbarWidth)
                .fillMaxHeight()
                .hoverable(interactionSource)
                .pointerHoverIcon(PointerIcon.Default)
                .pointerInput(canScroll, total, averageItem) {
                    if (!canScroll) return@pointerInput
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            scrollState.scrollBy(dragAmount.y / size.height * totalPixels)
                        }
                    }
                },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (!canScroll) return@Canvas
            val trackHeight = size.height
            val thumb =
                (trackHeight * visibleCount / total)
                    .coerceIn(minThumbHeight.toPx().coerceAtMost(trackHeight), trackHeight)
            val top = (trackHeight - thumb) * fraction
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
private fun AppVerticalScrollbarLazyPreview() {
    AppTheme(dark = true) {
        val state = androidx.compose.foundation.lazy.rememberLazyListState()
        val colors = LocalAppColors.current
        Box(Modifier.size(width = 280.dp, height = 180.dp)) {
            Row(Modifier.fillMaxSize()) {
                androidx.compose.foundation.lazy.LazyColumn(
                    state = state,
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                ) {
                    items(20) {
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
