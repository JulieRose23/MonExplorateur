package com.monexplorateur.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

enum class ModeTheme(val libelle: String) {
    SYSTEME("Système"),
    CLAIR("Clair"),
    SOMBRE("Sombre")
}

@Composable
fun MonExplorateurTheme(
    modeTheme: ModeTheme = ModeTheme.SYSTEME,
    content: @Composable () -> Unit
) {
    val darkTheme = when (modeTheme) {
        ModeTheme.SYSTEME -> isSystemInDarkTheme()
        ModeTheme.CLAIR -> false
        ModeTheme.SOMBRE -> true
    }
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
