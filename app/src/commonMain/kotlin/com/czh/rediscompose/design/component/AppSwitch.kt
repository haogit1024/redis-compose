package com.czh.rediscompose.design.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens

@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "appSwitchProgress",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) dimens.switchWidth - dimens.switchThumb - 2.dp else 2.dp,
        animationSpec = tween(durationMillis = 180),
        label = "appSwitchThumb",
    )
    val trackColor = lerp(colors.surface4, colors.accent, progress)
    val trackBorder = lerp(colors.borderStrong, Color.Transparent, progress)
    val thumbColor = lerp(colors.textSecondary, Color.White, progress)
    val trackShape = RoundedCornerShape(20.dp)
    val alpha = if (enabled) 1f else 0.45f

    Box(
        modifier =
            modifier
                .width(dimens.switchWidth)
                .height(dimens.switchHeight)
                .drawBehind {
                    if (progress > 0f) {
                        drawRoundRect(
                            color = colors.accentSofter.copy(alpha = colors.accentSofter.alpha * progress),
                            topLeft = Offset(-3.dp.toPx(), -3.dp.toPx()),
                            size =
                                Size(
                                    size.width + 6.dp.toPx(),
                                    size.height + 6.dp.toPx(),
                                ),
                            cornerRadius =
                                CornerRadius(
                                    (size.height + 6.dp.toPx()) / 2f,
                                ),
                        )
                    }
                }
                .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
                .background(trackColor.copy(alpha = trackColor.alpha * alpha), trackShape)
                .border(1.dp, trackBorder.copy(alpha = trackBorder.alpha * alpha), trackShape)
                .toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Switch,
                    onValueChange = onCheckedChange,
                ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier =
                Modifier
                    .offset(x = thumbOffset)
                    .size(dimens.switchThumb)
                    .background(thumbColor.copy(alpha = thumbColor.alpha * alpha), CircleShape),
        )
    }
}

@Preview
@Composable
private fun AppSwitchPreview() {
    AppTheme(dark = true) {
        Row(
            modifier =
                Modifier
                    .background(LocalAppColors.current.background)
                    .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppSwitch(checked = true, onCheckedChange = {})
            AppSwitch(checked = false, onCheckedChange = {})
            AppSwitch(checked = true, onCheckedChange = {}, enabled = false)
        }
    }
}
