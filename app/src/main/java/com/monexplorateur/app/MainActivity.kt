package com.monexplorateur.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.monexplorateur.app.ui.screens.ExplorateurRootScreen
import com.monexplorateur.app.ui.screens.ExplorateurShizukuScreen
import com.monexplorateur.app.ui.screens.ExplorateurStandardScreen
import com.monexplorateur.app.ui.screens.ReseauFtpScreen
import com.monexplorateur.app.ui.theme.ModeTheme
import com.monexplorateur.app.ui.theme.MonExplorateurTheme
import kotlinx.coroutines.launch

enum class SectionNavigation(val titre: String, val icone: ImageVector) {
    STOCKAGE("Stockage interne", Icons.Default.Search),
    SHIZUKU("Mode Shizuku", Icons.Default.Lock),
    ROOT("Accès Root (su)", Icons.Default.Build),
    FTP("Réseau FTP", Icons.Default.Share)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var modeTheme by remember { mutableStateOf(ModeTheme.SYSTEME) }
            MonExplorateurTheme(modeTheme = modeTheme) {
                ApplicationPrincipale(
                    modeTheme = modeTheme,
                    onChangerTheme = { modeTheme = it }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationPrincipale(
    modeTheme: ModeTheme,
    onChangerTheme: (ModeTheme) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var sectionActive by remember { mutableStateOf(SectionNavigation.STOCKAGE) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "MonExplorateur",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

                SectionNavigation.entries.forEach { section ->
                    NavigationDrawerItem(
                        icon = { Icon(section.icone, contentDescription = null) },
                        label = { Text(section.titre) },
                        selected = section == sectionActive,
                        onClick = {
                            sectionActive = section
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                HorizontalDivider()
                Text(
                    text = "Thème de l'interface",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 24.dp, top = 12.dp, bottom = 4.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    ModeTheme.entries.forEach { mode ->
                        FilterChip(
                            selected = modeTheme == mode,
                            onClick = { onChangerTheme(mode) },
                            label = { Text(mode.libelle) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(sectionActive.titre) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Ouvrir le menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (sectionActive) {
                    SectionNavigation.STOCKAGE -> ExplorateurStandardScreen()
                    SectionNavigation.SHIZUKU -> ExplorateurShizukuScreen()
                    SectionNavigation.ROOT -> ExplorateurRootScreen()
                    SectionNavigation.FTP -> ReseauFtpScreen()
                }
            }
        }
    }
}
