package com.czh.rediscompose.util

import kotlin.math.round

/**
 * Display formatters shared by every feature.
 *
 * All functions are pure and deterministic except [formatEpochSeconds], which uses the
 * platform default time zone (actual in `jvmMain`).
 */
fun formatTtl(seconds: Long): String =
    when {
        seconds == -1L -> "永久"
        seconds < 0L -> "已过期"
        seconds == 0L -> "即将过期"
        seconds < 60L -> "${seconds}s"
        seconds < 3_600L -> "${seconds / 60}m ${seconds % 60}s"
        seconds < 86_400L -> "${seconds / 3_600}h ${(seconds % 3_600) / 60}m"
        else -> "${seconds / 86_400}d ${(seconds % 86_400) / 3_600}h"
    }

/**
 * Condensed TTL used by key rows, e.g. `42s`, `28m`, `2h`, `3d`.
 *
 * Redis uses -1 for keys without expiry; those return "永久". Negative values other than
 * -1 mean the key is already gone.
 */
fun formatTtlCompact(seconds: Long): String =
    when {
        seconds == -1L -> "永久"
        seconds < 0L -> "已过期"
        seconds == 0L -> "0s"
        seconds < 60L -> "${seconds}s"
        seconds < 3_600L -> "${seconds / 60}m"
        seconds < 86_400L -> "${seconds / 3_600}h"
        else -> "${seconds / 86_400}d"
    }

fun formatBytes(bytes: Long?): String {
    if (bytes == null) return "—"
    if (bytes < 1_024L) return "$bytes B"
    val units = listOf("KB", "MB", "GB", "TB", "PB")
    var value = bytes.toDouble()
    var unitIndex = -1
    while (value >= 1_024.0 && unitIndex < units.lastIndex) {
        value /= 1_024.0
        unitIndex += 1
    }
    val text =
        when {
            value >= 100.0 -> round(value).toLong().toString()
            value >= 10.0 -> trimFraction(value, decimals = 1)
            else -> trimFraction(value, decimals = 2)
        }
    return "$text ${units[unitIndex]}"
}

fun formatCount(count: Long): String {
    val negative = count < 0L
    val digits = count.toString().removePrefix("-")
    val grouped = digits.reversed().chunked(3).joinToString(",").reversed()
    return if (negative) "-$grouped" else grouped
}

fun formatDurationMicros(micros: Long): String =
    when {
        micros < 1_000L -> "$micros µs"
        micros < 1_000_000L -> "${trimFraction(micros / 1_000.0, decimals = 1)} ms"
        else -> "${trimFraction(micros / 1_000_000.0, decimals = 2)} s"
    }

/** Local wall-clock time for a Redis epoch-seconds value. */
expect fun formatEpochSeconds(seconds: Long): String

/** Current Unix time in seconds; used for honest "read at" labels in the value pane. */
expect fun currentEpochSeconds(): Long

/** Current Unix time in milliseconds; used for session-local metric sampling. */
expect fun currentEpochMillis(): Long

/**
 * Cuts [text] to at most [maxBytes] UTF-8 bytes without splitting a code point.
 *
 * Used by the value editor to show a bounded preview of large strings.
 */
fun truncateUtf8(
    text: String,
    maxBytes: Int,
): String {
    if (maxBytes <= 0) return ""
    var bytes = 0
    var index = 0
    while (index < text.length) {
        val codePoint =
            if (text[index].isHighSurrogate() && index + 1 < text.length && text[index + 1].isLowSurrogate()) {
                val high = text[index].code
                val low = text[index + 1].code
                0x10000 + ((high - 0xD800) shl 10) + (low - 0xDC00)
            } else {
                text[index].code
            }
        val size =
            when {
                codePoint < 0x80 -> 1
                codePoint < 0x800 -> 2
                codePoint < 0x10000 -> 3
                else -> 4
            }
        if (bytes + size > maxBytes) break
        bytes += size
        index += if (codePoint >= 0x10000) 2 else 1
    }
    return text.substring(0, index)
}

private fun trimFraction(
    value: Double,
    decimals: Int,
): String {
    val factor = if (decimals == 1) 10.0 else 100.0
    val rounded = round(value * factor) / factor
    val text = rounded.toString()
    return text.removeSuffix(".0")
}
