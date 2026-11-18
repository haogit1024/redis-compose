package com.czh.rediscompose.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import kotlinx.coroutines.delay

@Composable
fun AppTooltip(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    var visible by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(dimens.radiusXs)
    val gap = with(density) { 6.dp.roundToPx() }
    val margin = with(density) { 8.dp.roundToPx() }

    LaunchedEffect(hovered, enabled) {
        if (hovered && enabled) {
            delay(500)
            visible = true
        } else {
            visible = false
        }
    }

    Box(
        modifier =
            modifier
                .wrapContentSize(Alignment.Center)
                .hoverable(interactionSource = interactionSource),
    ) {
        content()
        if (visible) {
            Popup(
                popupPositionProvider =
                    remember(gap, margin) {
                        object : PopupPositionProvider {
                            override fun calculatePosition(
                                anchorBounds: IntRect,
                                windowSize: IntSize,
                                layoutDirection: LayoutDirection,
                                popupContentSize: IntSize,
                            ): IntOffset {
                                val x =
                                    (anchorBounds.center.x - popupContentSize.width / 2)
                                        .coerceIn(margin, windowSize.width - popupContentSize.width - margin)
                                val y =
                                    (anchorBounds.top - popupContentSize.height - gap)
                                        .coerceAtLeast(margin)
                                return IntOffset(x, y)
                            }
                        }
                    },
                properties = PopupProperties(focusable = false),
            ) {
                Box(
                    modifier =
                        Modifier
                            .shadow(elevation = 8.dp, shape = shape)
                            .background(colors.surface4, shape)
                            .border(1.dp, colors.borderStrong, shape)
                            .padding(horizontal = dimens.spaceSm, vertical = 5.dp),
                ) {
                    Text(
                        text = text,
                        style = typography.tooltip,
                        color = colors.text,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun AppTooltipPreview() {
    AppTheme(dark = true) {
        Row(
            modifier =
                Modifier
                    .background(LocalAppColors.current.background)
                    .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppTooltip("刷新 Keyspace") {
                AppButton("悬停查看提示", {})
            }
            AppTooltip("切换主题") {
                AppIconButton(AppIcons.ThemeDark, "切换主题", {})
            }
        }
    }
}
