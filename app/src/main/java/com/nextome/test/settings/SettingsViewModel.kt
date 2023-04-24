package com.nextome.test.settings

import androidx.lifecycle.ViewModel

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
): ViewModel() {
    fun saveSettings(settings: AppOverriddenSettings) {
        settingsRepository.setUserSettings(settings)
    }

    fun clearSettings() {
        settingsRepository.clearSettings()
    }

    fun getCachedSettings(): AppOverriddenSettings? {
        return settingsRepository.getUserSettings()
    }
}