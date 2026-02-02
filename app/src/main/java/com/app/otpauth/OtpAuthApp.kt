package com.app.otpauth

import android.app.Application
import timber.log.Timber

class OtpAuthApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}
