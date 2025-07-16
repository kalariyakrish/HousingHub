package com.example.housinghub

import android.app.Application
import com.cloudinary.android.MediaManager

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val config = mapOf(
            "cloud_name" to "dp82wqtwj",
            "upload_preset" to "HousingHub"
        )
        MediaManager.init(this, config)
    }
}
