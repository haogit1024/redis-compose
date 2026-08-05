package com.czh.redis.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.czh.redis.ui.AppDimens
import com.czh.redis.ui.ConfirmRequest
import com.czh.redis.ui.LocalAppColors

@Composable
fun ConfirmDialog(
    request: ConfirmRequest?,
    onDismiss: () -> Unit,
) {
    if (request == null) return
    val colors = LocalAppColors.current

    AppDialog(
        visible = true,
        onDismiss = onDismiss,
        title = "确认操作",
        width = 420.dp,
        footer = {
            Spacer(modifier = Modifier.weight(1f))
            AppButton(
                text = "取消",
                variant = ButtonVariant.OUTLINE,
                onClick = onDismiss,
            )
            Spacer(modifier = Modifier.width(AppDimens.spacingMedium))
            AppButton(
                text = request.confirmText,
                variant = ButtonVariant.SOLID,
                onClick = {
                    onDismiss()
                    request.onConfirm()
                },
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = AppDimens.paddingMedium)) {
            Text(text = request.message, color = colors.text, fontSize = 13.sp)
        }
    }
}

@Composable
fun TtlEditDialog(
    title: String,
    initialTtl: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current
    var ttl by remember { mutableStateOf(initialTtl.toString()) }

    AppDialog(
        visible = true,
        onDismiss = onDismiss,
        title = title,
        width = 400.dp,
        footer = {
            Spacer(modifier = Modifier.weight(1f))
            AppButton(text = "取消", variant = ButtonVariant.OUTLINE, onClick = onDismiss)
            Spacer(modifier = Modifier.width(AppDimens.spacingMedium))
            AppButton(
                text = "确定",
                onClick = {
                    val parsed = ttl.toLongOrNull()
                    if (parsed != null) {
                        onDismiss()
                        onConfirm(parsed)
                    }
                },
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = AppDimens.paddingMedium)) {
            Text(text = "TTL（秒，-1 表示永久）", color = colors.textSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(8.dp))
            AppTextField(
                value = ttl,
                onValueChange = { ttl = it.filter { c -> c.isDigit() || c == '-' } },
                mono = true,
            )
            Text(
                text = "当前 TTL：$initialTtl 秒",
                color = colors.textSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
fun RenameDialog(
    title: String,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current
    var name by remember { mutableStateOf(initialName) }

    AppDialog(
        visible = true,
        onDismiss = onDismiss,
        title = title,
        width = 420.dp,
        footer = {
            Spacer(modifier = Modifier.weight(1f))
            AppButton(text = "取消", variant = ButtonVariant.OUTLINE, onClick = onDismiss)
            Spacer(modifier = Modifier.width(AppDimens.spacingMedium))
            AppButton(
                text = "确定",
                onClick = {
                    if (name.isNotBlank()) {
                        onDismiss()
                        onConfirm(name)
                    }
                },
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = AppDimens.paddingMedium)) {
            AppTextField(value = name, onValueChange = { name = it }, mono = true)
        }
    }
}
