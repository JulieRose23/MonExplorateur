package com.monexplorateur.app.data

import android.content.Context
import com.monexplorateur.app.ui.theme.ModeTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

enum class LangueApp(val code: String, val libelle: String) {
    FR("fr", "Français"),
    EN("en", "English")
}

object AppPreferences {
    private const val PREFS_NAME = "mon_explorateur_prefs"

    private val _modeTheme = MutableStateFlow(ModeTheme.SYSTEME)
    val modeTheme: StateFlow<ModeTheme> = _modeTheme

    private val _langue = MutableStateFlow(langueSystemeParDefaut())
    val langue: StateFlow<LangueApp> = _langue

    private val _rootActive = MutableStateFlow(false)
    val rootActive: StateFlow<Boolean> = _rootActive

    private val _shizukuActive = MutableStateFlow(false)
    val shizukuActive: StateFlow<Boolean> = _shizukuActive

    private fun langueSystemeParDefaut(): LangueApp {
        return if (Locale.getDefault().language.equals("fr", ignoreCase = true)) {
            LangueApp.FR
        } else {
            LangueApp.EN
        }
    }

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val themeName = prefs.getString("theme", ModeTheme.SYSTEME.name) ?: ModeTheme.SYSTEME.name
        _modeTheme.value = runCatching { ModeTheme.valueOf(themeName) }.getOrDefault(ModeTheme.SYSTEME)
        val defaut = langueSystemeParDefaut().name
        val langName = prefs.getString("langue", defaut) ?: defaut
        _langue.value = runCatching { LangueApp.valueOf(langName) }.getOrDefault(langueSystemeParDefaut())
        _rootActive.value = prefs.getBoolean("root_active", false)
        _shizukuActive.value = prefs.getBoolean("shizuku_active", false)
    }

    fun setModeTheme(context: Context, mode: ModeTheme) {
        _modeTheme.value = mode
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString("theme", mode.name).apply()
    }

    fun setLangue(context: Context, langue: LangueApp) {
        _langue.value = langue
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString("langue", langue.name).apply()
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
