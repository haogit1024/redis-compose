package com.czh.rediscompose.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.domain.settings.ThemeMode
import com.czh.rediscompose.feature.shell.AppShell

/**
 * Root composition: owns `AppState`, resolves the effective theme, and renders the shell.
 *
 * Phase 4+ replaces the placeholder content behind each workspace tab; the shell itself
 * stays here.
 */
@Composable
fun RedisComposeApp(container: AppContainer) {
    val scope = rememberCoroutineScope()
    val systemDark = isSystemInDarkTheme()
    val state =
        remember(container) {
            AppState(
                container = container,
                scope = scope,
                initialSettings = container.initialSettings,
                initialConnections = container.initialConnections,
            )
        }
    val dark =
        when (state.settings.theme) {
            ThemeMode.SYSTEM -> systemDark
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
        }
    AppTheme(dark = dark, accent = state.settings.accent) {
        DisposableEffect(state) {
            onDispose { state.close() }
        }
        AppShell(state = state, dark = dark)
    }
}
