package com.monexplorateur.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.monexplorateur.app.data.AppPreferences
import com.monexplorateur.app.ui.screens.ExplorateurUnifieScreen
import com.monexplorateur.app.ui.screens.ReglagesScreen
import com.monexplorateur.app.ui.screens.ReseauFtpScreen
import com.monexplorateur.app.ui.theme.MonExplorateurTheme

enum class OngletPrincipal(val titre: String, val icone: ImageVector) {
    STOCKAGE("Stockage", Icons.Default.Search),
    FTP("FTP", Icons.Default.Share),
    REGLAGES("Réglages", Icons.Default.Settings)
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
    val barreLiquid by AppPreferences.barreLiquid.collectAsState()

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
            if (barreLiquid) {
                BarreNavigationLiquid(
                    ongletActif = ongletActif,
                    onSelect = { ongletActif = it }
                )
            } else {
                NavigationBar {
                    OngletPrincipal.entries.forEach { onglet ->
                        NavigationBarItem(
                            selected = onglet == ongletActif,
                            onClick = { ongletActif = onglet },
                            icon = { Icon(onglet.icone, contentDescription = onglet.titre) },
                            label = { Text(onglet.titre) }
                        )
                    }
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

@Composable
private fun BarreNavigationLiquid(
    ongletActif: OngletPrincipal,
    onSelect: (OngletPrincipal) -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 10.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f),
            tonalElevation = 8.dp,
            shadowElevation = 10.dp,
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.45f),
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                    )
                )
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                OngletPrincipal.entries.forEach { onglet ->
                    val selectionne = onglet == ongletActif
                    val fondPilule by animateColorAsState(
                        targetValue = if (selectionne) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        label = "fondPilule"
                    )
                    val couleurContenu by animateColorAsState(
                        targetValue = if (selectionne) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "couleurContenu"
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(fondPilule)
                            .clickable { onSelect(onglet) }
                            .padding(horizontal = if (selectionne) 16.dp else 14.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = onglet.icone,
                            contentDescription = onglet.titre,
                            tint = couleurContenu,
                            modifier = Modifier.size(22.dp)
                        )
                        if (selectionne) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = onglet.titre,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = couleurContenu
                            )
                        }
                    }
                }
            }
        }
    }
}
