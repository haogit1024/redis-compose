package com.czh.rediscompose.design.component

import androidx.compose.ui.input.pointer.PointerIcon
import java.awt.Cursor

actual fun horizontalResizeCursor(): PointerIcon = PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR))
