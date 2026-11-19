package com.czh.rediscompose.util

/**
 * Shortcut text that stays legible with the bundled fonts.
 *
 * The Inter/JetBrains Mono bundles do not contain ⌘, ⇧ or ↵, so they render as tofu boxes.
 * Use plain words ("Cmd", "Shift", "Enter") instead of the glyphs.
 */
object ShortcutLabels {
    val command: String get() = if (isMacOs()) "Cmd" else "Ctrl"

    val shift: String get() = "Shift"

    fun commandKey(key: String): String = "$command $key"

    fun commandEnter(): String = "$command+Enter"

    fun commandKeys(vararg keys: String): List<String> = listOf(command) + keys
}
