package com.czh.redis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.czh.redis.ui.AppDimens
import com.czh.redis.ui.LocalAppColors

enum class ButtonVariant { SOLID, OUTLINE, GHOST, DANGER }

enum class ButtonSize { SMALL, MEDIUM }

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.SOLID,
    size: ButtonSize = ButtonSize.MEDIUM,
    enabled: Boolean = true,
    icon: String? = null,
) {
    val colors = LocalAppColors.current
    val bg: Color
    val contentColor: Color
    val borderColor: Color
    when (variant) {
        ButtonVariant.SOLID -> {
            bg = colors.primary
            contentColor = Color.White
            borderColor = Color.Transparent
        }
        ButtonVariant.OUTLINE -> {
            bg = colors.panel
            contentColor = colors.text
            borderColor = colors.border
        }
        ButtonVariant.GHOST -> {
            bg = Color.Transparent
            contentColor = colors.textSecondary
            borderColor = Color.Transparent
        }
        ButtonVariant.DANGER -> {
            bg = Color.Transparent
            contentColor = colors.error
            borderColor = colors.border
        }
    }
    val horizontalPad = if (size == ButtonSize.SMALL) 10.dp else 16.dp
    val verticalPad = if (size == ButtonSize.SMALL) 5.dp else 9.dp
    val fontSize = if (size == ButtonSize.SMALL) 12.sp else 13.sp
    val shape: Shape = RoundedCornerShape(AppDimens.cornerMedium)

    Box(
        modifier =
            modifier
                .background(if (enabled) bg else bg.copy(alpha = 0.5f), shape)
                .border(1.dp, borderColor, shape)
                .clickable(enabled = enabled) { onClick() }
                .padding(horizontal = horizontalPad, vertical = verticalPad),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = icon, fontSize = fontSize)
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = text,
                    color = if (enabled) contentColor else contentColor.copy(alpha = 0.7f),
                    fontSize = fontSize,
                )
            }
        } else {
            Text(
                text = text,
                color = if (enabled) contentColor else contentColor.copy(alpha = 0.7f),
                fontSize = fontSize,
            )
        }
    }
}

@Composable
fun IconButton(
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tooltip: String = "",
    size: Dp = 24.dp,
) {
    val colors = LocalAppColors.current
    Box(
        modifier =
            modifier
                .size(size)
                .clickable { onClick() }
                .padding(3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = icon, color = colors.textSecondary, fontSize = 12.sp)
    }
}
