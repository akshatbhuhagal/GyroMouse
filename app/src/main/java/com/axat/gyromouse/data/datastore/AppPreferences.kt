package com.axat.gyromouse.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.axat.gyromouse.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** DataStore instance extension on Context */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bt_mouse_settings")

/**
 * Wrapper around Jetpack DataStore for persisting user preferences.
 * Each setting has a corresponding PreferencesKey and a default value.
 */
@Singleton
class AppPreferences @Inject constructor(
    private val context: Context
) {

    companion object {
        // Preference keys
        val SENSITIVITY = floatPreferencesKey("sensitivity")
        val SCROLL_SPEED = floatPreferencesKey("scroll_speed")
        val NATURAL_SCROLL = booleanPreferencesKey("natural_scroll")
        val TAP_TO_CLICK = booleanPreferencesKey("tap_to_click")
        val SHOW_GESTURE_HINTS = booleanPreferencesKey("show_gesture_hints")
        val KEEP_SCREEN_AWAKE = booleanPreferencesKey("keep_screen_awake")
        val POINTER_ACCELERATION = booleanPreferencesKey("pointer_acceleration")
    }

    /**
     * Observe all settings as a Flow of AppSettings.
     * Emits a new AppSettings whenever any preference changes.
     */
    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            sensitivity = prefs[SENSITIVITY] ?: 1.5f,
            scrollSpeed = prefs[SCROLL_SPEED] ?: 1.0f,
            naturalScroll = prefs[NATURAL_SCROLL] ?: false,
            tapToClick = prefs[TAP_TO_CLICK] ?: true,
            showGestureHints = prefs[SHOW_GESTURE_HINTS] ?: true,
            keepScreenAwake = prefs[KEEP_SCREEN_AWAKE] ?: true,
            pointerAcceleration = prefs[POINTER_ACCELERATION] ?: true
        )
    }

    /** Update the mouse sensitivity multiplier */
    suspend fun setSensitivity(value: Float) {
        context.dataStore.edit { it[SENSITIVITY] = value.coerceIn(0.5f, 3.0f) }
    }

    /** Update the scroll speed multiplier */
    suspend fun setScrollSpeed(value: Float) {
        context.dataStore.edit { it[SCROLL_SPEED] = value.coerceIn(0.5f, 3.0f) }
    }

    /** Toggle natural (inverted) scroll direction */
    suspend fun setNaturalScroll(enabled: Boolean) {
        context.dataStore.edit { it[NATURAL_SCROLL] = enabled }
    }

    /** Toggle tap-to-click feature */
    suspend fun setTapToClick(enabled: Boolean) {
        context.dataStore.edit { it[TAP_TO_CLICK] = enabled }
    }

    /** Toggle gesture hint overlay visibility */
    suspend fun setShowGestureHints(show: Boolean) {
        context.dataStore.edit { it[SHOW_GESTURE_HINTS] = show }
    }

    /** Toggle screen wake lock during trackpad use */
    suspend fun setKeepScreenAwake(enabled: Boolean) {
        context.dataStore.edit { it[KEEP_SCREEN_AWAKE] = enabled }
    }

    /** Toggle pointer acceleration */
    suspend fun setPointerAcceleration(enabled: Boolean) {
        context.dataStore.edit { it[POINTER_ACCELERATION] = enabled }
    }
}
