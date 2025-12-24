package com.example.smartmosque

import android.app.Application
import com.cloudinary.android.MediaManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SmartMosqueApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Inisialisasi Cloudinary
        val config = HashMap<String, String>()
        config["cloud_name"] = "dhzn4vwic"
        config["secure"] = "true"

        MediaManager.init(this, config)
    }
}
