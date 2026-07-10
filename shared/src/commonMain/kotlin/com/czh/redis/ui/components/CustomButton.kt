package com.czh.redis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.czh.redis.ui.AppColors
import com.czh.redis.ui.AppDimens

@Composable
fun CustomButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (BoxScope.() -> Unit)? = null,
    backgroundColor: Color = AppColors.primary,
    contentColor: Color = Color.White,
    borderColor: Color = Color.Transparent,
    shape: Shape = RoundedCornerShape(AppDimens.cornerMedium),
) {
    Box(
        modifier = modifier
            .background(color = backgroundColor, shape = shape)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .clickable { onClick() }
            .padding(AppDimens.paddingMedium),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                    icon()
                }
                Spacer(modifier = Modifier.size(AppDimens.spacingSmall))
                Text(text = text, color = contentColor)
            }
        } else {
            Text(text = text, color = contentColor)
        }
    }
}


@Composable
fun NewConnectionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CustomButton(
        text = "New Connection",
        onClick = onClick,
        modifier = modifier,
//        icon = { PlusIcon(color = Color.White) },
        backgroundColor = Color(0xFF6B7280),
        contentColor = Color.White,
        shape = RoundedCornerShape(AppDimens.cornerSmall),
    )
}
