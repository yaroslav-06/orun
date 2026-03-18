package com.yarick.orunner

import android.app.Application
import com.google.android.material.color.DynamicColors

class OrunApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
