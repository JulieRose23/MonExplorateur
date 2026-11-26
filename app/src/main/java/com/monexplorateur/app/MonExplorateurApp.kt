package com.monexplorateur.app

import android.app.Application
import com.monexplorateur.app.data.AppPreferences
import com.monexplorateur.app.root.RootManager
import com.monexplorateur.app.shizuku.ShizukuManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MonExplorateurApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        AppPreferences.init(this)
        ShizukuManager.init(this, demanderSiActif = AppPreferences.shizukuActive.value)
        if (AppPreferences.rootActive.value) {
            appScope.launch {
                RootManager.verifierEtConnecter(this@MonExplorateurApp)
            }
        }
    }
}
