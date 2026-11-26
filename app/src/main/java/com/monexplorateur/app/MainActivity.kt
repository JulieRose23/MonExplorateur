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
import androidx.compose.material.icons.filled.Search
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
import com.monexplorateur.app.ui.components.IconeDossierModerne
import com.monexplorateur.app.ui.screens.ExplorateurUnifieScreen
import com.monexplorateur.app.ui.screens.ReglagesScreen
import com.monexplorateur.app.ui.screens.ReseauFtpScreen
import com.monexplorateur.app.ui.theme.MonExplorateurTheme

enum class OngletPrincipal(val titre: String) {
    STOCKAGE("Stockage"),
    FTP("FTP"),
    REGLAGES("Réglages")
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (ongletActif) {
                            OngletPrincipal.STOCKAGE -> "MonExplorateur"
                            OngletPrincipal.FTP -> "Réseau FTP"
                            OngletPrincipal.REGLAGES -> "Réglages"
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
                        OngletPrincipal.FTP -> Icons.Default.Share
                        OngletPrincipal.REGLAGES -> Icons.Default.Settings
                    }
                    NavigationBarItem(
                        selected = onglet == ongletActif,
                        onClick = { ongletActif = onglet },
                        icon = { Icon(icone, contentDescription = onglet.titre) },
                        label = { Text(onglet.titre) }
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
                OngletPrincipal.FTP -> Box(modifier = Modifier.padding(basPadding)) { ReseauFtpScreen() }
                OngletPrincipal.REGLAGES -> ReglagesScreen(paddingBasNavigation = basPadding)
            }
        }
    }
}
