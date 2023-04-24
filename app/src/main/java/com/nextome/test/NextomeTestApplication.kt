package com.nextome.test

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.nextome.test.DiModules.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin

class NextomeTestApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@NextomeTestApplication)
            modules(appModule)
        }
    }
}