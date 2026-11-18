package com.czh.rediscompose.design.component

import androidx.compose.ui.input.pointer.PointerIcon

/**
 * Horizontal resize cursor used by draggable splitters.
 *
 * Compose common does not expose a resize cursor, so each platform supplies one. Unknown
 * platforms fall back to the hand cursor.
 */
expect fun horizontalResizeCursor(): PointerIcon
