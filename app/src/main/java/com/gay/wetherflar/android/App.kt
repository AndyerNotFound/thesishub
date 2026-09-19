package com.gay.wetherflar.android

import android.app.Application
import com.gay.wetherflar.android.data.Store
import com.google.android.material.color.DynamicColors

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        
        DynamicColors.applyToActivitiesIfAvailable(this)
        Store.init(this)
    }
}
