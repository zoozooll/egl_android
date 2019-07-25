package com.aaron.egllib

import android.app.Application

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppHolder.init(this)
    }

    override fun onTerminate() {
        super.onTerminate()
    }
}
