package com.czh.rediscompose.design.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens

enum class AppIconButtonSize { Small, Medium }

@Composable
fun AppIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: AppIconButtonSize = AppIconButtonSize.Medium,
    active: Boolean = false,
    enabled: Boolean = true,
    tint: Color? = null,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    var focused by remember { mutableStateOf(false) }
    val hoverProgress by animateFloatAsState(
        targetValue = if (hovered && enabled) 1f else 0f,
        animationSpec = tween(durationMillis = 140),
        label = "appIconButtonHover",
    )
    val isSmall = size == AppIconButtonSize.Small
    val boxSize = if (isSmall) dimens.iconButtonSizeSmall else dimens.iconButtonSize
    val shape = RoundedCornerShape(if (isSmall) dimens.radiusControlSmall else dimens.radiusControl)
    val background =
        when {
            active -> colors.accentSoft
            else -> lerp(colors.surface3.copy(alpha = 0f), colors.surface3, hoverProgress)
        }
    val contentColor =
        when {
            !enabled -> colors.textTertiary.copy(alpha = 0.45f)
            tint != null -> tint
            active -> colors.accent
            else -> lerp(colors.textTertiary, colors.text, hoverProgress)
        }

    Box(
        modifier =
            modifier
                .size(boxSize)
                .clip(shape)
                .background(background)
                .border(1.dp, if (focused && enabled) colors.accent else Color.Transparent, shape)
                .onFocusChanged { focused = it.isFocused }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                )
                .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
                .padding(if (isSmall) 5.dp else 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(if (isSmall) dimens.iconSm else dimens.iconButtonIcon),
        )
    }
}

@Preview
@Composable
private fun AppIconButtonPreview() {
    AppTheme(dark = true) {
        Row(
            modifier =
                Modifier
                    .background(LocalAppColors.current.background)
                    .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIconButton(AppIcons.Refresh, "刷新", {})
            AppIconButton(AppIcons.Star, "收藏", {}, active = true)
            AppIconButton(AppIcons.More, "更多", {}, size = AppIconButtonSize.Small)
            AppIconButton(AppIcons.Delete, "删除", {}, enabled = false)
        }
    }
}
