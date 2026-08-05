package com.czh.redis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.czh.redis.ui.LocalAppColors
import com.czh.redis.ui.Toast
import com.czh.redis.ui.ToastType

@Composable
fun ToastHost(
    toasts: List<Toast>,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.End,
    ) {
        toasts.forEach { toast ->
            val borderColor = when (toast.type) {
                ToastType.SUCCESS -> colors.success
                ToastType.ERROR -> colors.error
                ToastType.WARNING -> colors.warn
                ToastType.INFO -> colors.primary
            }
            Row(
                modifier = Modifier
                    .widthIn(min = 260.dp, max = 420.dp)
                    .background(colors.panel, RoundedCornerShape(8.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                    .border(3.dp, borderColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "●", color = borderColor, fontSize = 10.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = toast.message, color = colors.text, fontSize = 12.sp)
                    if (toast.detail != null) {
                        Spacer(modifier = Modifier.width(0.dp))
                        Text(
                            text = "详情：${toast.detail.lineSequence().firstOrNull() ?: ""}",
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 2,
                        )
                    }
                }
            }
        }
    }
}
