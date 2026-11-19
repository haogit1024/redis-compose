package com.czh.rediscompose.util

/**
 * Heuristic for Hash fields whose values should be masked by default.
 *
 * This is deliberately a name match only: Redis has no schema, so the field name is the
 * only signal available. The pattern follows the Phase 9 contract
 * (`password|passwd|secret|token|api[_-]?key|private`).
 */
object SensitiveFields {
    const val MASK = "••••••"

    private val pattern = Regex("password|passwd|secret|token|api[_-]?key|private", RegexOption.IGNORE_CASE)

    fun isSensitive(field: String): Boolean = pattern.containsMatchIn(field)

    fun maskedOrPlain(
        value: String,
        masked: Boolean,
    ): String = if (masked) MASK else value
}
