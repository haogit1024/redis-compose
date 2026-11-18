package com.czh.rediscompose.domain.settings

/**
 * Theme preference values shared by the design system and the future settings domain.
 *
 * They live in `domain/settings` so Phase 2's `AppSettings` can reference them directly
 * instead of defining a second copy.
 */
enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT,
}

enum class Accent {
    IRIS,
    MINT,
    SKY,
    AMBER,
    ROSE,
}

enum class UiDensity {
    COMFORTABLE,
    COMPACT,
}
