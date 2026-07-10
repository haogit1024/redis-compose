package com.czh.redis

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.redis.ui.Left
import com.czh.redis.ui.Right
import com.czh.redis.ui.components.NewConnectionDialog
import java.awt.Cursor

@Composable
@Preview
fun App() {
    var leftPanelWidth by remember { mutableStateOf(300f) }
    var showDialog by remember { mutableStateOf(false) }
    val dividerWidth = 4f
    val density = LocalDensity.current

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .width(leftPanelWidth.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.TopStart,
            ) {
                Left(onNewConnectionClick = { showDialog = true })
            }

            Box(
                modifier = Modifier
                    .width(dividerWidth.dp)
                    .fillMaxHeight()
                    .background(Color.Gray)
                    .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaDp = with(density) { dragAmount.x.toDp().value }
                            leftPanelWidth = (leftPanelWidth + deltaDp).coerceIn(100f, 800f)
                        }
                    }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                Right()
            }
        }

        NewConnectionDialog(
            visible = showDialog,
            onDismiss = { showDialog = false },
            onConfirm = { showDialog = false },
        )
    }
}