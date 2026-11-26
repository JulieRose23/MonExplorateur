package com.monexplorateur.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.monexplorateur.app.data.AppPreferences
import com.monexplorateur.app.data.LangueApp
import com.monexplorateur.app.ui.components.IconeDossierModerne
import com.monexplorateur.app.ui.screens.CorbeilleScreen
import com.monexplorateur.app.ui.screens.ExplorateurUnifieScreen
import com.monexplorateur.app.ui.screens.ReglagesScreen
import com.monexplorateur.app.ui.screens.ReseauFtpScreen
import com.monexplorateur.app.ui.theme.MonExplorateurTheme

enum class OngletPrincipal {
    STOCKAGE, CORBEILLE, FTP, REGLAGES;

    fun titre(en: Boolean): String = when (this) {
        STOCKAGE -> if (en) "Storage" else "Stockage"
        CORBEILLE -> if (en) "Trash" else "Corbeille"
        FTP -> "FTP"
        REGLAGES -> if (en) "Settings" else "Réglages"
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val modeTheme by AppPreferences.modeTheme.collectAsState()
            MonExplorateurTheme(modeTheme = modeTheme) {
                ApplicationPrincipale()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationPrincipale() {
    var ongletActif by remember { mutableStateOf(OngletPrincipal.STOCKAGE) }
    val langue by AppPreferences.langue.collectAsState()
    val en = langue == LangueApp.EN

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (ongletActif) {
                            OngletPrincipal.STOCKAGE -> "MonExplorateur"
                            OngletPrincipal.CORBEILLE -> if (en) "Trash" else "Corbeille"
                            OngletPrincipal.FTP -> if (en) "FTP Network" else "Réseau FTP"
                            OngletPrincipal.REGLAGES -> if (en) "Settings" else "Réglages"
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                OngletPrincipal.entries.forEach { onglet ->
                    val icone: ImageVector = when (onglet) {
                        OngletPrincipal.STOCKAGE -> IconeDossierModerne
                        OngletPrincipal.CORBEILLE -> Icons.Default.Delete
                        OngletPrincipal.FTP -> Icons.Default.Share
                        OngletPrincipal.REGLAGES -> Icons.Default.Settings
                    }
                    NavigationBarItem(
                        selected = onglet == ongletActif,
                        onClick = { ongletActif = onglet },
                        icon = { Icon(icone, contentDescription = onglet.titre(en)) },
                        label = { Text(onglet.titre(en)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            val basPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding())
            when (ongletActif) {
                OngletPrincipal.STOCKAGE -> ExplorateurUnifieScreen(paddingBasNavigation = basPadding)
                OngletPrincipal.CORBEILLE -> CorbeilleScreen(paddingBasNavigation = basPadding)
                OngletPrincipal.FTP -> Box(modifier = Modifier.padding(basPadding)) { ReseauFtpScreen() }
                OngletPrincipal.REGLAGES -> ReglagesScreen(paddingBasNavigation = basPadding)
            }
        }
    }
}
