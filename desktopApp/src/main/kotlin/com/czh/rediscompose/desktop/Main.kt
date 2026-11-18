package com.czh.rediscompose.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.czh.rediscompose.app.RedisComposeApp
import java.awt.Dimension

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Redis Compose",
            state = WindowState(size = DpSize(1440.dp, 900.dp)),
        ) {
            window.minimumSize = Dimension(1120, 720)
            RedisComposeApp()
        }
    }
}
