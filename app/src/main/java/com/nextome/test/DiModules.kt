package com.nextome.test

import android.content.Context
import android.content.SharedPreferences
import com.nextome.test.map.MapViewModel
import com.nextome.test.settings.SettingsRepository
import com.nextome.test.settings.SettingsRepositoryImpl
import com.nextome.test.settings.SettingsViewModel
import com.nextome.test.utils.batteryOptimization.BatteryManager
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

object DiModules {
    val appModule = module {
        single<SharedPreferences> { androidContext().getSharedPreferences("nextome_db", Context.MODE_PRIVATE) }
        single<BatteryManager> { BatteryManager(get()) }
        single<SettingsRepository> { SettingsRepositoryImpl(androidContext()) }
        viewModel { SplashScreenViewModel(get(), get()) }
        viewModel { MapViewModel(get(), get()) }
        viewModel { SettingsViewModel(get()) }
    }
}