package com.nextome.test

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextome.test.settings.SettingsRepository
import com.nextome.test.utils.batteryOptimization.BatteryManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SplashScreenViewModel(
    private val settingsRepository: SettingsRepository,
    private val batteryManager: BatteryManager,
): ViewModel() {
    private val _uiEvents = MutableSharedFlow<UiEvent?>(
        replay = 2,
        extraBufferCapacity = 2,
    )
    val uiEvents = _uiEvents.filterNotNull()

    init {
        viewModelScope.launch {
            reloadSettings()
        }
    }

    fun reloadSettings() {
        _uiEvents.tryEmit(ShowHasEditedSetting(
            settingsRepository.getUserSettings()?.hasEditedSettings() == true))
    }

    fun checkBatteryOptimizations(ctx: Context) {
        batteryManager.warnIfBatteryOptimized(ctx)
    }

    sealed class UiEvent
    data class ShowMessageEvent(val message: String): UiEvent()
    object ShowLoading: UiEvent()
    object HideLoading: UiEvent()
    data class ShowHasEditedSetting(val settingsEdited: Boolean): UiEvent()
}