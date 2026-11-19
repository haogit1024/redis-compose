package com.czh.rediscompose.design.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.czh.rediscompose.domain.settings.Accent
import com.czh.rediscompose.domain.settings.UiDensity

val LocalAppColors =
    staticCompositionLocalOf<AppColors> {
        error("AppColors is not available outside AppTheme")
    }

val LocalAppDimens =
    staticCompositionLocalOf<AppDimens> {
        error("AppDimens is not available outside AppTheme")
    }

val LocalAppTypography =
    staticCompositionLocalOf<AppTypography> {
        error("AppTypography is not available outside AppTheme")
    }

@Composable
fun AppTheme(
    dark: Boolean,
    accent: Accent = Accent.IRIS,
    density: UiDensity = UiDensity.COMFORTABLE,
    content: @Composable () -> Unit,
) {
    val colors = remember(dark, accent) { AppColors.forTheme(dark, accent) }
    val dimens = remember(density) { AppDimens.forDensity(density) }
    val typography = rememberAppTypography()
    val textInputFocus = remember { TextInputFocusTracker() }
    val shapes = remember(dimens) { appShapes(dimens) }
    val materialTypography =
        remember(typography) {
            Typography(
                displayLarge = typography.display,
                displayMedium = typography.display,
                displaySmall = typography.pageTitle,
                headlineLarge = typography.pageTitle,
                headlineMedium = typography.pageTitle,
                headlineSmall = typography.sectionTitle,
                titleLarge = typography.sectionTitle,
                titleMedium = typography.bodyStrong,
                titleSmall = typography.bodyStrong,
                bodyLarge = typography.body,
                bodyMedium = typography.body,
                bodySmall = typography.caption,
                labelLarge = typography.button,
                labelMedium = typography.caption,
                labelSmall = typography.microLabel,
            )
        }
    val colorScheme =
        remember(colors) {
            if (colors.isDark) {
                darkColorScheme(
                    primary = colors.accent,
                    onPrimary = colors.accentInk,
                    primaryContainer = colors.accentSoft,
                    onPrimaryContainer = colors.accent,
                    secondary = colors.textSecondary,
                    onSecondary = colors.background,
                    background = colors.background,
                    onBackground = colors.text,
                    surface = colors.surface,
                    onSurface = colors.text,
                    surfaceVariant = colors.surface2,
                    onSurfaceVariant = colors.textSecondary,
                    outline = colors.borderStrong,
                    outlineVariant = colors.border,
                    error = colors.danger,
                    onError = Color.White,
                    scrim = colors.scrim,
                )
            } else {
                lightColorScheme(
                    primary = colors.accent,
                    onPrimary = colors.accentInk,
                    primaryContainer = colors.accentSoft,
                    onPrimaryContainer = colors.accent,
                    secondary = colors.textSecondary,
                    onSecondary = colors.surface,
                    background = colors.background,
                    onBackground = colors.text,
                    surface = colors.surface,
                    onSurface = colors.text,
                    surfaceVariant = colors.surface2,
                    onSurfaceVariant = colors.textSecondary,
                    outline = colors.borderStrong,
                    outlineVariant = colors.border,
                    error = colors.danger,
                    onError = Color.White,
                    scrim = colors.scrim,
                )
            }
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = materialTypography,
        shapes = shapes,
    ) {
        CompositionLocalProvider(
            LocalAppColors provides colors,
            LocalAppDimens provides dimens,
            LocalAppTypography provides typography,
            LocalTextInputFocus provides textInputFocus,
            LocalTextStyle provides typography.body.copy(color = colors.text),
            LocalContentColor provides colors.text,
            content = content,
        )
    }
}
