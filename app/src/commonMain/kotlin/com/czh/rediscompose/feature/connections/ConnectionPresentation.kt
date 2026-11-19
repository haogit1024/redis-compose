package com.czh.rediscompose.feature.connections

import androidx.compose.ui.graphics.Color
import com.czh.rediscompose.app.SessionStatus
import com.czh.rediscompose.design.theme.AppColors
import com.czh.rediscompose.domain.connection.ConnectionColor
import com.czh.rediscompose.domain.connection.ConnectionEnvironment
import com.czh.rediscompose.domain.connection.ConnectionProfile

internal fun ConnectionEnvironment.displayName(): String =
    when (this) {
        ConnectionEnvironment.PRODUCTION -> "Production"
        ConnectionEnvironment.STAGING -> "Staging"
        ConnectionEnvironment.LOCAL -> "Local"
        ConnectionEnvironment.ANALYTICS -> "Analytics"
        ConnectionEnvironment.OTHER -> "Other"
    }

internal fun ConnectionProfile.initials(): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        words.isEmpty() -> "RC"
        words.size >= 2 -> words.take(2).map { it.first() }.joinToString("").uppercase()
        else -> words.first().take(2).uppercase()
    }
}

internal fun ConnectionColor.toColor(colors: AppColors): Color =
    when (this) {
        ConnectionColor.IRIS -> colors.accent
        ConnectionColor.MINT -> colors.success
        ConnectionColor.AMBER -> colors.warning
        ConnectionColor.SKY -> colors.info
        ConnectionColor.ROSE -> colors.typeZset
        ConnectionColor.LILAC -> colors.typeList
    }

internal fun ConnectionRuntime.stateLabel(): String =
    when (status) {
        SessionStatus.CONNECTED -> latencyMillis?.let { "$it ms" } ?: "已连接"
        SessionStatus.CONNECTING -> "连接中"
        SessionStatus.ERROR -> "连接失败"
        SessionStatus.DISCONNECTED -> "未连接"
    }

internal fun ConnectionRuntime.stateColor(colors: AppColors): Color =
    when (status) {
        SessionStatus.CONNECTED -> colors.success
        SessionStatus.CONNECTING -> colors.warning
        SessionStatus.ERROR -> colors.danger
        SessionStatus.DISCONNECTED -> colors.textTertiary
    }
