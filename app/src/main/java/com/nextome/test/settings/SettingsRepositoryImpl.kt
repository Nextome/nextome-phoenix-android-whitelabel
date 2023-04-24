package com.nextome.test.settings

import android.content.Context
import com.nextome.test.helper.NmSerialization.asJson
import com.nextome.test.helper.NmSerialization.fromJson

class SettingsRepositoryImpl(
    context: Context,
): SettingsRepository {
    companion object {
        const val KEY_SETTINGS = "nextome_settings"
    }

    private val sharedPref = context.getSharedPreferences("nextome_db", Context.MODE_PRIVATE)

    override fun getUserSettings(): AppOverriddenSettings? {
        with (sharedPref) {
            val serializedSettings = getString(KEY_SETTINGS, null) ?: return null
            return serializedSettings.fromJson()
        }
    }

    override fun setUserSettings(settings: AppOverriddenSettings) {
        with (sharedPref.edit()) {
            putString(KEY_SETTINGS, settings.asJson())
            commit()
        }
    }

    override fun clearSettings() {
        with (sharedPref.edit()) {
            putString(KEY_SETTINGS, null)
            commit()
        }
    }
}