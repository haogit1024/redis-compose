package com.czh.rediscompose.feature.shell

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import com.czh.rediscompose.app.AppPage
import com.czh.rediscompose.app.AppState
import com.czh.rediscompose.app.WorkspaceTabKind
import com.czh.rediscompose.domain.settings.ThemeMode

/**
 * Application-wide keyboard handling from `plan/09-settings-palette.md` §7.
 *
 * Modified shortcuts are handled in the preview phase because a text field never treats
 * ⌘/Ctrl combinations as typed input. Plain `N`/`R` are handled in the bubbling phase so
 * they only fire when no text field consumed the character.
 */
internal fun Modifier.globalShortcuts(
    state: AppState,
    dark: Boolean,
    isTextInputFocused: () -> Boolean,
): Modifier =
    this
        .onPreviewKeyEvent { event -> state.handleModifiedShortcut(event, dark) }
        .onKeyEvent { event -> state.handlePlainShortcut(event, isTextInputFocused) }

private fun AppState.handleModifiedShortcut(
    event: KeyEvent,
    dark: Boolean,
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    val command = event.isMetaPressed || event.isCtrlPressed
    if (!command) return false
    if (event.key == Key.K) {
        if (!commandPaletteVisible && hasBlockingOverlay) return false
        toggleCommandPalette()
        return true
    }
    if (commandPaletteVisible || hasBlockingOverlay) return false
    val onWorkspace = activePage == AppPage.WORKSPACE
    val workspace = activeWorkspace()
    val console = activeConsole()
    return when {
        event.key == Key.Comma -> {
            openSettings()
            true
        }
        event.key == Key.F && onWorkspace && workspace != null -> {
            workspace.requestSearchFocus()
            true
        }
        event.key == Key.S && onWorkspace && workspace != null -> {
            workspace.requestSave()
            true
        }
        event.key == Key.W && onWorkspace && activeTab() != null -> {
            activeTabId?.let { closeTab(it) }
            true
        }
        event.key == Key.L && event.isShiftPressed -> {
            // SYSTEM resolves to the current effective theme first, then becomes explicit.
            updateTheme(if (dark) ThemeMode.LIGHT else ThemeMode.DARK)
            true
        }
        (event.key == Key.Enter || event.key == Key.NumPadEnter) && onWorkspace && console != null -> {
            if (console.input.isNotBlank() && !console.isRunning) console.execute(console.input)
            true
        }
        else -> false
    }
}

private fun AppState.handlePlainShortcut(
    event: KeyEvent,
    isTextInputFocused: () -> Boolean,
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    if (event.key == Key.Escape && commandPaletteVisible) {
        setPaletteVisible(false)
        return true
    }
    if (commandPaletteVisible || hasBlockingOverlay) return false
    if (isTextInputFocused()) return false
    val plain =
        !event.isMetaPressed &&
            !event.isCtrlPressed &&
            !event.isAltPressed &&
            !event.isShiftPressed
    if (!plain) return false
    if (activePage != AppPage.WORKSPACE) return false
    if (activeTab()?.kind != WorkspaceTabKind.KEYS) return false
    val workspace = activeWorkspace() ?: return false
    return when (event.key) {
        Key.N -> {
            workspace.requestNewKey()
            true
        }
        Key.R -> {
            workspace.refresh()
            true
        }
        else -> false
    }
}
