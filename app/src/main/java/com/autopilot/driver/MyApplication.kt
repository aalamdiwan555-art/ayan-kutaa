package com.autopilot.driver

import android.app.Application

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        runCatching { AppPrefs.init(this) }
        AdManager.init(this)
    }
}
