package com.axat.gyromouse.data.repository

import com.axat.gyromouse.data.datastore.AppPreferences
import com.axat.gyromouse.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for user settings. Provides a reactive Flow of settings
 * and methods to update individual preferences.
 *
 * Wraps [AppPreferences] (DataStore) to keep the domain layer
 * decoupled from the storage implementation.
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val appPreferences: AppPreferences
) {

    /** Observe all settings as a continuous Flow */
    val settings: Flow<AppSettings> = appPreferences.settingsFlow

    /** Update mouse sensitivity (0.5x – 3.0x) */
    suspend fun setSensitivity(value: Float) =
        appPreferences.setSensitivity(value)

    /** Update scroll speed (0.5x – 3.0x) */
    suspend fun setScrollSpeed(value: Float) =
        appPreferences.setScrollSpeed(value)

    /** Toggle natural (inverted) scrolling */
    suspend fun setNaturalScroll(enabled: Boolean) =
        appPreferences.setNaturalScroll(enabled)

    /** Toggle tap-to-click */
    suspend fun setTapToClick(enabled: Boolean) =
        appPreferences.setTapToClick(enabled)

    /** Toggle gesture hint overlay */
    suspend fun setShowGestureHints(show: Boolean) =
        appPreferences.setShowGestureHints(show)

    /** Toggle keep-screen-awake */
    suspend fun setKeepScreenAwake(enabled: Boolean) =
        appPreferences.setKeepScreenAwake(enabled)

    /** Toggle pointer acceleration */
    suspend fun setPointerAcceleration(enabled: Boolean) =
        appPreferences.setPointerAcceleration(enabled)
}
