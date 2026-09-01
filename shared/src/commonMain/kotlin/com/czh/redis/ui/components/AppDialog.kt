package com.czh.redis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.czh.redis.ui.AppDimens
import com.czh.redis.ui.LocalAppColors

@Composable
fun AppDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    width: Dp = 520.dp,
    maxHeight: Dp = 560.dp,
    footer: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (!visible) return
    val colors = LocalAppColors.current

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            ),
    ) {
        Column(
            modifier =
                modifier
                    .width(width)
                    .heightIn(max = maxHeight)
                    .background(colors.panel, RoundedCornerShape(AppDimens.cornerDialog))
                    .border(1.dp, colors.border, RoundedCornerShape(AppDimens.cornerDialog))
                    .padding(AppDimens.paddingLarge),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = title, color = colors.text, fontSize = 15.sp)
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier =
                        Modifier
                            .size(24.dp)
                            .clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "✕", color = colors.textSecondary, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.size(AppDimens.paddingMedium))

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
            ) {
                content()
            }

            if (footer != null) {
                Spacer(modifier = Modifier.size(AppDimens.paddingMedium))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    footer()
                }
            }
        }
    }
}
