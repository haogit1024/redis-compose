package com.czh.rediscompose.design.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged

/**
 * Counts currently focused text inputs.
 *
 * Global plain-key shortcuts (`N` / `R`) must not fire while the user is typing, and key
 * events alone are not enough to know that reliably on every platform. Every text field
 * reports focus through [trackTextInputFocus].
 */
class TextInputFocusTracker {
    var focusedCount by mutableStateOf(0)
        private set

    val isTextInputFocused: Boolean get() = focusedCount > 0

    internal fun update(focused: Boolean) {
        focusedCount = (focusedCount + if (focused) 1 else -1).coerceAtLeast(0)
    }
}

val LocalTextInputFocus = staticCompositionLocalOf { TextInputFocusTracker() }

/** Marks the receiving text field as an input so global letter shortcuts back off. */
@Composable
fun Modifier.trackTextInputFocus(): Modifier {
    val tracker = LocalTextInputFocus.current
    var focused by remember { mutableStateOf(false) }
    DisposableEffect(tracker) {
        onDispose {
            if (focused) tracker.update(false)
        }
    }
    return onFocusChanged { state ->
        if (state.isFocused != focused) {
            focused = state.isFocused
            tracker.update(state.isFocused)
        }
    }
}
