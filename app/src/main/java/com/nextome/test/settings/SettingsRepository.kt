package com.nextome.test.settings

interface SettingsRepository {
    fun getUserSettings(): AppOverriddenSettings?
    fun setUserSettings(settings: AppOverriddenSettings)
    fun clearSettings()
}