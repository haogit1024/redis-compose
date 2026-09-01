package com.czh.redis.data

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val theme: String = "system",
    val fontFamily: String = "JetBrains Mono",
    val fontSize: Int = 13,
    val batchSize: Int = 1000,
    val valueTruncateKb: Int = 10,
    val defaultView: String = "tree",
    val readonly: Boolean = false,
    val encryptPassword: Boolean = true,
    val language: String = "zh-CN",
) {
    fun isDark(platformDark: Boolean): Boolean =
        when (theme) {
            "light" -> false
            "dark" -> true
            else -> platformDark
        }
}
