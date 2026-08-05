package com.czh.redis.ui

import androidx.compose.ui.input.pointer.PointerIcon
import java.awt.Cursor

actual fun resizeHPointerIcon(): PointerIcon =
    PointerIcon(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR))
