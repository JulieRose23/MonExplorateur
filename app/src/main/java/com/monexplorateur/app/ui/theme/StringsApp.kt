package com.monexplorateur.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.monexplorateur.app.data.AppPreferences
import com.monexplorateur.app.data.LangueApp

object S {
    @Composable
    fun isEn(): Boolean {
        val lang by AppPreferences.langue.collectAsState()
        return lang == LangueApp.EN
    }

    fun t(fr: String, en: String, isEn: Boolean): String = if (isEn) en else fr
}
