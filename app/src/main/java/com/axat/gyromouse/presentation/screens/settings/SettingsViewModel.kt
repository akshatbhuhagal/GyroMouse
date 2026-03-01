package com.axat.gyromouse.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axat.gyromouse.data.repository.SettingsRepository
import com.axat.gyromouse.domain.model.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Settings screen.
 * Observes and updates all user preferences via SettingsRepository.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun setSensitivity(value: Float) {
        viewModelScope.launch { settingsRepository.setSensitivity(value) }
    }

    fun setScrollSpeed(value: Float) {
        viewModelScope.launch { settingsRepository.setScrollSpeed(value) }
    }

    fun setNaturalScroll(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setNaturalScroll(enabled) }
    }

    fun setTapToClick(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setTapToClick(enabled) }
    }

    fun setShowGestureHints(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowGestureHints(show) }
    }

    fun setKeepScreenAwake(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setKeepScreenAwake(enabled) }
    }

    fun setPointerAcceleration(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setPointerAcceleration(enabled) }
    }
}
