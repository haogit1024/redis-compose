package com.czh.redis.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class AppColorTokens(
    val primary: Color,
    val background: Color,
    val panel: Color,
    val panelAlt: Color,
    val text: Color,
    val textSecondary: Color,
    val border: Color,
    val hover: Color,
    val selectedBg: Color,
    val success: Color,
    val error: Color,
    val warn: Color,
    val scrollbar: Color,
    val tabBg: Color,
)

object AppThemeTokens {
    val light =
        AppColorTokens(
            primary = Color(0xFFDC382C),
            background = Color(0xFFFAFAFA),
            panel = Color(0xFFFFFFFF),
            panelAlt = Color(0xFFF2F3F5),
            text = Color(0xFF212121),
            textSecondary = Color(0xFF757575),
            border = Color(0xFFE5E7EB),
            hover = Color(0x14DC382C),
            selectedBg = Color(0x14DC382C),
            success = Color(0xFF22A45D),
            error = Color(0xFFD32F2F),
            warn = Color(0xFFF59E0B),
            scrollbar = Color(0xFFD0D0D0),
            tabBg = Color(0xFFF1F1F1),
        )

    val dark =
        AppColorTokens(
            primary = Color(0xFFE05B4B),
            background = Color(0xFF1E1F22),
            panel = Color(0xFF26272B),
            panelAlt = Color(0xFF2E3035),
            text = Color(0xFFE4E4E7),
            textSecondary = Color(0xFF9E9EA3),
            border = Color(0xFF3A3B40),
            hover = Color(0x1FE05B4B),
            selectedBg = Color(0x1FE05B4B),
            success = Color(0xFF34C772),
            error = Color(0xFFF06A6A),
            warn = Color(0xFFF5A623),
            scrollbar = Color(0xFF4A4B50),
            tabBg = Color(0xFF1B1C1F),
        )
}

val LocalAppColors = staticCompositionLocalOf { AppThemeTokens.light }

object TypeColors {
    val string = Color(0xFF3B82F6)
    val hash = Color(0xFFF59E0B)
    val list = Color(0xFF8B5CF6)
    val set = Color(0xFF10B981)
    val zset = Color(0xFFEC4899)
    val stream = Color(0xFF06B6D4)
    val none = Color(0xFF9E9EA3)

    fun forType(type: com.czh.redis.redis.RedisType): Color =
        when (type) {
            com.czh.redis.redis.RedisType.STRING -> string
            com.czh.redis.redis.RedisType.HASH -> hash
            com.czh.redis.redis.RedisType.LIST -> list
            com.czh.redis.redis.RedisType.SET -> set
            com.czh.redis.redis.RedisType.ZSET -> zset
            com.czh.redis.redis.RedisType.STREAM -> stream
            com.czh.redis.redis.RedisType.NONE -> none
        }
}

object AppDimens {
    val cornerSmall = 4.dp
    val cornerMedium = 6.dp
    val cornerLarge = 8.dp
    val cornerDialog = 12.dp
    val paddingSmall = 8.dp
    val paddingMedium = 12.dp
    val paddingLarge = 16.dp
    val spacingSmall = 4.dp
    val spacingMedium = 8.dp
    val spacingLarge = 12.dp
    val rowHeight = 32.dp
    val tableRowHeight = 28.dp
}

@Composable
fun AppTheme(
    dark: Boolean,
    content: @Composable () -> Unit,
) {
    val colors = if (dark) AppThemeTokens.dark else AppThemeTokens.light
    val colorScheme =
        if (dark) {
            darkColorScheme(
                primary = colors.primary,
                onPrimary = Color.White,
                background = colors.background,
                onBackground = colors.text,
                surface = colors.panel,
                onSurface = colors.text,
                surfaceVariant = colors.panelAlt,
                onSurfaceVariant = colors.textSecondary,
                outline = colors.border,
                outlineVariant = colors.border,
                error = colors.error,
                onError = Color.White,
            )
        } else {
            lightColorScheme(
                primary = colors.primary,
                onPrimary = Color.White,
                background = colors.background,
                onBackground = colors.text,
                surface = colors.panel,
                onSurface = colors.text,
                surfaceVariant = colors.panelAlt,
                onSurfaceVariant = colors.textSecondary,
                outline = colors.border,
                outlineVariant = colors.border,
                error = colors.error,
                onError = Color.White,
            )
        }
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
