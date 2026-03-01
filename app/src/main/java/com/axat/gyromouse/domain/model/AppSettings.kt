package com.axat.gyromouse.domain.model

/**
 * Data class representing all user-configurable app settings.
 * Persisted via DataStore and exposed through SettingsRepository.
 */
data class AppSettings(
    /** Mouse movement sensitivity multiplier (0.5x to 3.0x) */
    val sensitivity: Float = 1.5f,

    /** Scroll speed multiplier (0.5x to 3.0x) */
    val scrollSpeed: Float = 1.0f,

    /** Whether to use natural (inverted) scrolling like macOS */
    val naturalScroll: Boolean = false,

    /** Whether tapping the trackpad registers as a click */
    val tapToClick: Boolean = true,

    /** Whether to show the gesture hint overlay on first use */
    val showGestureHints: Boolean = true,

    /** Whether to keep the screen awake while the trackpad is active */
    val keepScreenAwake: Boolean = true,

    /** Whether to apply pointer acceleration for fast movements */
    val pointerAcceleration: Boolean = true
)
