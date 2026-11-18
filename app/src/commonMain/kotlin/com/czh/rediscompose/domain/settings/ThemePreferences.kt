package com.czh.rediscompose.domain.settings

import kotlinx.serialization.Serializable

/**
 * Theme preference values shared by the design system and the settings domain.
 *
 * They live in `domain/settings` so `AppSettings` can reference them directly instead of
 * defining a second copy.
 */
@Serializable
enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT,
}

@Serializable
enum class Accent {
    IRIS,
    MINT,
    SKY,
    AMBER,
    ROSE,
}

@Serializable
enum class UiDensity {
    COMFORTABLE,
    COMPACT,
}
