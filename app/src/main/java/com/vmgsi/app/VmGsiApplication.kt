package com.vmgsi.app

import android.app.Application

class VmGsiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Global init hook — DI setup would go here if the project grows
        // beyond simple constructor injection.
    }
}
