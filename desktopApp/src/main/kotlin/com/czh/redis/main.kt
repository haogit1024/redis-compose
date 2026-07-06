package com.czh.redis

import androidx.compose.material.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "redis-compose",
    ) {
        MaterialTheme {
            App()
        }
    }
}