package com.czh.rediscompose.design.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography

enum class AppButtonVariant { Primary, Secondary, Ghost, Danger }

enum class AppButtonSize { Small, Medium }

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AppButtonVariant = AppButtonVariant.Secondary,
    size: AppButtonSize = AppButtonSize.Medium,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    var focused by remember { mutableStateOf(false) }

    val isSmall = size == AppButtonSize.Small
    val height = if (isSmall) dimens.buttonHeightSmall else dimens.buttonHeight
    val horizontalPadding = if (isSmall) dimens.buttonPaddingSmall else dimens.buttonPadding
    val shape = RoundedCornerShape(if (isSmall) dimens.radiusControlSmall else dimens.radiusControl)
    val textStyle: TextStyle = if (isSmall) typography.buttonSmall else typography.button
    val iconSize = if (isSmall) 13.dp else dimens.iconSm
    val interactive = enabled && !loading

    val baseBackground =
        when (variant) {
            AppButtonVariant.Primary -> colors.accentStrong
            AppButtonVariant.Secondary -> colors.surface2
            AppButtonVariant.Ghost -> Color.Transparent
            AppButtonVariant.Danger -> colors.danger.copy(alpha = 0.12f)
        }
    val hoverBackground =
        when (variant) {
            AppButtonVariant.Primary -> colors.accentStrong
            AppButtonVariant.Secondary -> colors.surface3
            AppButtonVariant.Ghost -> colors.surface2
            AppButtonVariant.Danger -> colors.danger.copy(alpha = 0.18f)
        }
    val hoverProgress by animateFloatAsState(
        targetValue = if (hovered && interactive) 1f else 0f,
        animationSpec = tween(durationMillis = 140),
        label = "appButtonHover",
    )
    val background = lerp(baseBackground, hoverBackground, hoverProgress)
    val borderColor =
        when {
            focused && interactive -> colors.accent
            variant == AppButtonVariant.Primary -> Color.White.copy(alpha = if (hovered && interactive) 0.22f else 0.14f)
            variant == AppButtonVariant.Secondary -> if (hovered && interactive) colors.borderEmphasis else colors.border
            variant == AppButtonVariant.Ghost -> Color.Transparent
            else -> colors.danger.copy(alpha = 0.26f)
        }
    val contentColor =
        when (variant) {
            AppButtonVariant.Primary -> colors.accentInk
            AppButtonVariant.Secondary -> colors.text
            AppButtonVariant.Ghost -> lerp(colors.textSecondary, colors.text, hoverProgress)
            AppButtonVariant.Danger -> colors.danger
        }.let { if (enabled) it else it.copy(alpha = 0.45f) }
    val pressOffset by animateDpAsState(
        targetValue = if (pressed && interactive) 0.5.dp else 0.dp,
        animationSpec = tween(durationMillis = 80),
        label = "appButtonPress",
    )
    val primaryHoverAlpha by animateFloatAsState(
        targetValue = if (hovered && interactive && variant == AppButtonVariant.Primary) 0.07f else 0f,
        animationSpec = tween(durationMillis = 140),
        label = "appButtonPrimaryHover",
    )

    Box(
        modifier =
            modifier
                .height(height)
                .graphicsLayer {
                    translationY = pressOffset.toPx()
                    alpha = if (enabled) 1f else 0.55f
                }
                .clip(shape)
                .then(
                    if (variant == AppButtonVariant.Primary) {
                        Modifier.background(
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        lerp(colors.accent, Color.White, 0.22f),
                                        colors.accentStrong,
                                    ),
                            ),
                        )
                    } else {
                        Modifier.background(background)
                    },
                )
                .border(1.dp, borderColor, shape)
                .onFocusChanged { focused = it.isFocused }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = interactive,
                    role = Role.Button,
                    onClick = onClick,
                )
                .then(if (interactive) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
                .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        if (primaryHoverAlpha > 0f) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(Color.White.copy(alpha = primaryHoverAlpha)),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.iconTextGap),
        ) {
            when {
                loading ->
                    CircularProgressIndicator(
                        modifier = Modifier.size(iconSize),
                        color = contentColor,
                        strokeWidth = 1.5.dp,
                    )
                icon != null ->
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(iconSize),
                        tint = contentColor,
                    )
            }
            Text(
                text = text,
                style = textStyle,
                color = contentColor,
                maxLines = 1,
            )
        }
    }
}

@Preview
@Composable
private fun AppButtonPreview() {
    AppTheme(dark = true) {
        Column(
            modifier =
                Modifier
                    .background(LocalAppColors.current.background)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppButton("主要操作", {}, variant = AppButtonVariant.Primary, icon = AppIcons.Save)
                AppButton("次要操作", {}, variant = AppButtonVariant.Secondary)
                AppButton("幽灵", {}, variant = AppButtonVariant.Ghost)
                AppButton("删除", {}, variant = AppButtonVariant.Danger, icon = AppIcons.Delete)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppButton("小按钮", {}, size = AppButtonSize.Small, icon = AppIcons.Add)
                AppButton("小按钮", {}, size = AppButtonSize.Small, variant = AppButtonVariant.Primary)
                AppButton("禁用", {}, enabled = false)
                AppButton("加载中", {}, loading = true, variant = AppButtonVariant.Primary)
            }
        }
    }
}

@Preview
@Composable
private fun AppButtonLightPreview() {
    AppTheme(dark = false) {
        Row(
            modifier =
                Modifier
                    .background(LocalAppColors.current.background)
                    .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppButton("保存", {}, variant = AppButtonVariant.Primary, icon = AppIcons.Save)
            AppButton("取消", {})
            AppButton("删除", {}, variant = AppButtonVariant.Danger)
        }
    }
}
