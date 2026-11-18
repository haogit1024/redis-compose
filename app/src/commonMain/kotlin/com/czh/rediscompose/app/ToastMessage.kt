package com.czh.rediscompose.app

import androidx.compose.runtime.mutableStateListOf
import com.czh.rediscompose.design.component.ToastKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ToastMessage(
    val id: Long,
    val message: String,
    val kind: ToastKind = ToastKind.Info,
    val detail: String? = null,
)

/**
 * Owns the toast queue for the app shell.
 *
 * The host renders at most three; each toast disappears after three seconds. Keeping the
 * queue here (instead of inside a screen) means connection errors stay visible while the
 * user navigates.
 */
class ToastController(
    private val scope: CoroutineScope,
    private val durationMillis: Long = 3_000,
    private val maxVisible: Int = 3,
) {
    val toasts = mutableStateListOf<ToastMessage>()

    private var nextId = 0L

    fun show(
        message: String,
        kind: ToastKind = ToastKind.Info,
        detail: String? = null,
    ) {
        nextId += 1
        val toast = ToastMessage(id = nextId, message = message, kind = kind, detail = detail)
        if (toasts.size >= maxVisible) toasts.removeAt(0)
        toasts.add(toast)
        scope.launch {
            delay(durationMillis)
            dismiss(toast.id)
        }
    }

    fun dismiss(id: Long) {
        toasts.removeAll { it.id == id }
    }

    fun clear() {
        toasts.clear()
    }
}
