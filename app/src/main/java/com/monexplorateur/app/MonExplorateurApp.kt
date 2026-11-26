package com.monexplorateur.app

import android.app.Application
import com.monexplorateur.app.shizuku.ShizukuManager

class MonExplorateurApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ShizukuManager.initialiser()
    }
}
