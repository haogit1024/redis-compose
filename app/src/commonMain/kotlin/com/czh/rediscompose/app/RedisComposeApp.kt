package com.czh.rediscompose.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.czh.rediscompose.design.gallery.DesignGalleryScreen
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.domain.settings.Accent

/**
 * Phase 1 entry point: the app opens on the design-system gallery.
 *
 * Phase 3 replaces the gallery with `AppState` + the real workspace shell.
 */
@Composable
fun RedisComposeApp() {
    var dark by remember { mutableStateOf(true) }
    var accent by remember { mutableStateOf(Accent.IRIS) }
    AppTheme(dark = dark, accent = accent) {
        DesignGalleryScreen(
            dark = dark,
            accent = accent,
            onDarkChange = { dark = it },
            onAccentChange = { accent = it },
        )
    }
}

@Preview(widthDp = 1280, heightDp = 900)
@Composable
private fun RedisComposeAppPreview() {
    RedisComposeApp()
}
