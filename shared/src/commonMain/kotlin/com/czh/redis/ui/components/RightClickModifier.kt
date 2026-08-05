package com.czh.redis.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow

@Composable
fun rememberRightClickHandler(onRightClick: (Offset) -> Unit): Modifier {
    var nodePos by remember { mutableStateOf(Offset.Zero) }
    return Modifier
        .onGloballyPositioned { nodePos = it.positionInWindow() }
        .pointerInput(nodePos) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                        val position = event.changes.firstOrNull()?.position ?: continue
                        onRightClick(nodePos + position)
                    }
                }
            }
        }
}

fun formatCount(count: Long): String {
    if (count == 0L) return "0"
    val s = count.toString()
    return buildString {
        var counter = 0
        for (i in s.indices.reversed()) {
            if (counter == 3) {
                insert(0, ',')
                counter = 0
            }
            insert(0, s[i])
            counter++
        }
    }
}
