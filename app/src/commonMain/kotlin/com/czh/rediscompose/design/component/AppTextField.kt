package com.czh.rediscompose.design.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.design.theme.trackTextInputFocus

/**
 * Text input used everywhere in the app.
 *
 * The focus ring is drawn outside the control bounds, so focusing a field never changes
 * the layout. Placeholder, leading icon, trailing slot, password and multiline modes are
 * all configurable through this single component.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    singleLine: Boolean = true,
    isPassword: Boolean = false,
    mono: Boolean = false,
    minHeight: Dp? = null,
    height: Dp? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    onFocusChanged: ((Boolean) -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    var focused by remember { mutableStateOf(false) }
    val glowAlpha by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(durationMillis = 140),
        label = "appTextFieldFocus",
    )

    val shape = RoundedCornerShape(dimens.radiusControl)
    val fieldHeight = if (singleLine) (height ?: dimens.controlHeight) else (minHeight ?: dimens.multilineMinHeight)
    val borderColor =
        when {
            !enabled -> colors.border
            isError -> colors.danger
            focused -> colors.accentLine
            hovered -> colors.borderStrong
            else -> colors.border
        }
    val glowColor =
        (if (isError) colors.danger else colors.accent).copy(
            alpha = (if (isError) 0.08f else 0.06f) * glowAlpha,
        )
    val textStyle =
        (if (mono) typography.monoBody else typography.input).copy(
            color = if (enabled) colors.text else colors.textTertiary,
        )
    val startPadding = if (leadingIcon != null) dimens.fieldLeadingPadding else dimens.fieldPadding
    val endPadding = if (trailingContent != null) dimens.iconButtonSize else dimens.fieldPadding

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier =
            modifier
                .fillMaxWidth()
                .onFocusChanged {
                    focused = it.isFocused
                    onFocusChanged?.invoke(it.isFocused)
                }
                .trackTextInputFocus()
                .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Text) else Modifier),
        enabled = enabled,
        singleLine = singleLine,
        textStyle = textStyle,
        cursorBrush = SolidColor(colors.accent),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation =
            if (isPassword) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            if (glowAlpha > 0f) {
                                val glow = dimens.fieldGlow.toPx()
                                drawRoundRect(
                                    color = glowColor,
                                    topLeft = Offset(-glow, -glow),
                                    size = Size(size.width + glow * 2, size.height + glow * 2),
                                    cornerRadius = CornerRadius(dimens.radiusControl.toPx() + glow),
                                )
                            }
                        }
                        .heightIn(min = fieldHeight)
                        .clip(shape)
                        .background(colors.surface2)
                        .border(1.dp, borderColor, shape),
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier =
                            Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = dimens.fieldPadding)
                                .size(dimens.iconSm),
                    )
                }
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = fieldHeight)
                            .padding(
                                start = startPadding,
                                end = endPadding,
                                top = if (singleLine) 0.dp else 9.dp,
                                bottom = if (singleLine) 0.dp else 9.dp,
                            ),
                    contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
                ) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            style = textStyle.copy(color = colors.textQuaternary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
                if (trailingContent != null) {
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 6.dp),
                    ) {
                        trailingContent()
                    }
                }
            }
        },
    )
}

@Preview
@Composable
private fun AppTextFieldPreview() {
    AppTheme(dark = true) {
        Column(
            modifier =
                Modifier
                    .background(LocalAppColors.current.background)
                    .padding(16.dp)
                    .width(360.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AppTextField(
                value = "prod-cache",
                onValueChange = {},
                placeholder = "连接名称",
            )
            AppTextField(
                value = "",
                onValueChange = {},
                placeholder = "搜索 Key…",
                leadingIcon = AppIcons.Search,
            )
            AppTextField(
                value = "secret",
                onValueChange = {},
                isPassword = true,
                placeholder = "密码",
                trailingContent = { AppKbd("Enter") },
            )
            AppTextField(
                value = "",
                onValueChange = {},
                isError = true,
                placeholder = "必填项",
            )
            AppTextField(
                value = "disabled",
                onValueChange = {},
                enabled = false,
            )
            AppTextField(
                value = "{\n  \"user\": \"1001\"\n}",
                onValueChange = {},
                singleLine = false,
                mono = true,
            )
        }
    }
}
