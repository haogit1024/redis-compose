package com.czh.redis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.czh.redis.ui.AppDimens
import com.czh.redis.ui.LocalAppColors

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    mono: Boolean = false,
    password: Boolean = false,
    readOnly: Boolean = false,
    height: Dp = 32.dp,
    fontSize: Int = 13,
    singleLine: Boolean = true,
    onPreviewKeyEvent: (androidx.compose.ui.input.key.KeyEvent) -> Boolean = { false },
) {
    val colors = LocalAppColors.current
    val textStyle = TextStyle(
        color = colors.text,
        fontSize = fontSize.sp,
        fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = textStyle,
        cursorBrush = SolidColor(colors.primary),
        readOnly = readOnly,
        singleLine = singleLine,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = modifier
            .fillMaxWidth()
            .background(colors.panel, RoundedCornerShape(AppDimens.cornerMedium))
            .border(1.dp, colors.border, RoundedCornerShape(AppDimens.cornerMedium))
            .padding(horizontal = AppDimens.paddingSmall, vertical = 0.dp)
            .onPreviewKeyEvent(onPreviewKeyEvent),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxWidth().height(height),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    androidx.compose.material3.Text(
                        text = placeholder,
                        color = colors.textSecondary,
                        fontSize = fontSize.sp,
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
fun AppMultilineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    mono: Boolean = true,
    minHeight: Dp = 120.dp,
) {
    val colors = LocalAppColors.current
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(
            color = colors.text,
            fontSize = 12.sp,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
        ),
        cursorBrush = SolidColor(colors.primary),
        modifier = modifier
            .fillMaxWidth()
            .background(colors.panel, RoundedCornerShape(AppDimens.cornerMedium))
            .border(1.dp, colors.border, RoundedCornerShape(AppDimens.cornerMedium))
            .padding(AppDimens.paddingMedium),
        decorationBox = { innerTextField ->
            Box(modifier = Modifier.fillMaxWidth().height(minHeight)) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    androidx.compose.material3.Text(
                        text = placeholder,
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                    )
                }
                innerTextField()
            }
        },
    )
}
