package com.czh.redis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.czh.redis.ui.LocalAppColors
import com.czh.redis.ui.Toast
import com.czh.redis.ui.ToastType
import kotlinx.coroutines.launch

@Composable
fun ToastHost(
    toasts: List<Toast>,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    var detailToast by remember { mutableStateOf<Toast?>(null) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.End,
    ) {
        toasts.forEach { toast ->
            val borderColor =
                when (toast.type) {
                    ToastType.SUCCESS -> colors.success
                    ToastType.ERROR -> colors.error
                    ToastType.WARNING -> colors.warn
                    ToastType.INFO -> colors.primary
                }
            Row(
                modifier =
                    Modifier
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
                    SelectionContainer {
                        Text(text = toast.message, color = colors.text, fontSize = 12.sp)
                    }
                    if (toast.detail != null) {
                        Spacer(modifier = Modifier.width(0.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SelectionContainer(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "详情：${toast.detail.lineSequence().firstOrNull() ?: ""}",
                                    color = colors.textSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 2,
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "查看详情",
                                color = colors.primary,
                                fontSize = 11.sp,
                                modifier =
                                    Modifier
                                        .clickable { detailToast = toast }
                                        .padding(2.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    detailToast?.let { toast ->
        ErrorDetailDialog(
            toast = toast,
            onDismiss = { detailToast = null },
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ErrorDetailDialog(
    toast: Toast,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    AppDialog(
        visible = true,
        onDismiss = onDismiss,
        title = "错误详情",
        width = 640.dp,
        maxHeight = 480.dp,
        footer = {
            Spacer(modifier = Modifier.weight(1f))
            AppButton(
                text = "复制",
                variant = ButtonVariant.OUTLINE,
                size = ButtonSize.SMALL,
                onClick = {
                    scope.launch {
                        clipboard.setClipEntry(
                            ClipEntry(
                                AnnotatedString(
                                    buildString {
                                        append(toast.message)
                                        toast.detail?.let {
                                            append("\n\n")
                                            append(it)
                                        }
                                    },
                                ),
                            ),
                        )
                    }
                },
            )
            Spacer(modifier = Modifier.width(8.dp))
            AppButton(
                text = "关闭",
                size = ButtonSize.SMALL,
                onClick = onDismiss,
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SelectionContainer {
                Text(text = toast.message, color = colors.text, fontSize = 12.sp)
            }
            toast.detail?.let { detail ->
                Spacer(modifier = Modifier.width(0.dp))
                SelectionContainer(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = detail,
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}
