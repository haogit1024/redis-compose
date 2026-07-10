package com.czh.redis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.czh.redis.ui.AppDimens

@Composable
fun CustomDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (!visible) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x80000000))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .background(Color.White, shape = RoundedCornerShape(AppDimens.cornerLarge))
                .padding(AppDimens.paddingLarge)
                .clickable(enabled = false) {},
        ) {
            content()
        }
    }
}