package com.czh.rediscompose.util

/**
 * Bounds and normalization for settings that are entered as numbers or short strings.
 *
 * Keeping the limits in one pure object lets the settings UI, state holders and tests agree
 * on the same values without duplicating magic numbers.
 */
object SettingsLimits {
    val EDITOR_FONT_SIZE = 11..18
    val SCAN_BATCH_SIZE = 100..10_000
    val TRUNCATE_VALUE_KB = 1..1_024
    const val KEY_SEPARATOR_MAX_LENGTH = 3

    fun editorFontSize(value: Int): Int = value.coerceIn(EDITOR_FONT_SIZE)

    fun scanBatchSize(value: Int): Int = value.coerceIn(SCAN_BATCH_SIZE)

    fun truncateValueKb(value: Int): Int = value.coerceIn(TRUNCATE_VALUE_KB)

    /** Empty separators are invalid because the key tree would silently become flat. */
    fun keySeparator(value: String): String = value.take(KEY_SEPARATOR_MAX_LENGTH).ifEmpty { ":" }
}
