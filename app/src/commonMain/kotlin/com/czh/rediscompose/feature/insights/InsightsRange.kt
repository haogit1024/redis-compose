package com.czh.rediscompose.feature.insights

/**
 * Time windows available on the insights page.
 *
 * Redis exposes no historical data, so every window is filtered from the samples collected
 * in this session. [SESSION] therefore shows everything still retained (at most one hour).
 */
enum class InsightsRange(
    val label: String,
    val seconds: Long,
) {
    LAST_5_MINUTES("5M", 300),
    LAST_15_MINUTES("15M", 900),
    LAST_1_HOUR("1H", 3_600),
    SESSION("会话", Long.MAX_VALUE),
}
