package com.monexplorateur.app.data

import android.content.Context
import com.monexplorateur.app.ui.theme.ModeTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AppPreferences {
    private const val PREFS_NAME = "mon_explorateur_prefs"

    private val _modeTheme = MutableStateFlow(ModeTheme.SYSTEME)
    val modeTheme: StateFlow<ModeTheme> = _modeTheme

    private val _rootActive = MutableStateFlow(false)
    val rootActive: StateFlow<Boolean> = _rootActive

    private val _shizukuActive = MutableStateFlow(true)
    val shizukuActive: StateFlow<Boolean> = _shizukuActive

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val themeName = prefs.getString("theme", ModeTheme.SYSTEME.name) ?: ModeTheme.SYSTEME.name
        _modeTheme.value = runCatching { ModeTheme.valueOf(themeName) }.getOrDefault(ModeTheme.SYSTEME)
        _rootActive.value = prefs.getBoolean("root_active", false)
        _shizukuActive.value = prefs.getBoolean("shizuku_active", true)
    }

    fun setModeTheme(context: Context, mode: ModeTheme) {
        _modeTheme.value = mode
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString("theme", mode.name).apply()
    }

    fun setRootActive(context: Context, actif: Boolean) {
        _rootActive.value = actif
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean("root_active", actif).apply()
    }

    fun setShizukuActive(context: Context, actif: Boolean) {
        _shizukuActive.value = actif
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean("shizuku_active", actif).apply()
    }
}
