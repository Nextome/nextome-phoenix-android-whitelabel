package com.nextome.test

import android.app.Application
import com.bugsnag.android.Bugsnag

class PhoenixApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        Bugsnag.start(this)
    }
}