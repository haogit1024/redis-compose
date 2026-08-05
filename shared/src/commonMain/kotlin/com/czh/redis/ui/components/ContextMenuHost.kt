package com.czh.redis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.czh.redis.ui.AppDimens
import com.czh.redis.ui.ContextMenuState
import com.czh.redis.ui.LocalAppColors

@Composable
fun ContextMenuHost(menuState: ContextMenuState) {
    if (!menuState.visible) return
    val colors = LocalAppColors.current

    Popup(
        offset = IntOffset(
            menuState.x.toInt(),
            menuState.y.toInt(),
        ),
        onDismissRequest = { menuState.dismiss() },
    ) {
        Column(
            modifier = Modifier
                .width(180.dp)
                .background(colors.panel, RoundedCornerShape(AppDimens.cornerLarge))
                .padding(5.dp),
        ) {
            menuState.items.forEach { item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            menuState.dismiss()
                            item.onClick()
                        }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = item.label,
                        color = if (item.danger) colors.error else colors.text,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}
