package com.monexplorateur.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.monexplorateur.app.data.AppPreferences
import com.monexplorateur.app.root.RootManager
import com.monexplorateur.app.shizuku.ShizukuManager
import com.monexplorateur.app.ui.theme.ModeTheme
import kotlinx.coroutines.launch

@Composable
fun ReglagesScreen(paddingBasNavigation: PaddingValues = PaddingValues(0.dp)) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val modeTheme by AppPreferences.modeTheme.collectAsState()
    val rootActive by AppPreferences.rootActive.collectAsState()
    val shizukuActive by AppPreferences.shizukuActive.collectAsState()
    val barreLiquid by AppPreferences.barreLiquid.collectAsState()

    val shizukuDispo by ShizukuManager.estDisponible.collectAsState()
    val shizukuPerm by ShizukuManager.aPermission.collectAsState()
    val rootStatut by RootManager.aAccesRoot.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingBasNavigation)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Apparence & Barre Liquid
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Apparence & Navigation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Barre flottante Liquid Glass", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Pilule flottante translucide moderne en bas de l'écran",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = barreLiquid,
                        onCheckedChange = { AppPreferences.setBarreLiquid(context, it) }
                    )
                }

                HorizontalDivider()

                Text("Thème de l'application", style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeTheme.entries.forEach { mode ->
                        FilterChip(
                            selected = modeTheme == mode,
                            onClick = { AppPreferences.setModeTheme(context, mode) },
                            label = { Text(mode.libelle) }
                        )
                    }
                }
            }
        }

        // Section Privilèges & Accès Système (Shizuku & Root)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Accès Système & Privilèges", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                // Switch Shizuku
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mode Shizuku (/Android/data & obb)", style = MaterialTheme.typography.bodyLarge)
                        val etatShizuku = when {
                            !shizukuActive -> "Désactivé"
                            shizukuDispo && shizukuPerm -> "Connecté et autorisé"
                            shizukuDispo -> "Service actif — Autorisation requise"
                            else -> "Application Shizuku non démarrée"
                        }
                        Text(
                            text = etatShizuku,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = shizukuActive,
                        onCheckedChange = { actif ->
                            AppPreferences.setShizukuActive(context, actif)
                            if (actif) ShizukuManager.verifierEtat()
                        }
                    )
                }

                HorizontalDivider()

                // Switch Root (su)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Accès Superutilisateur (Root / su)", style = MaterialTheme.typography.bodyLarge)
                        val etatRoot = when {
                            !rootActive -> "Désactivé (navigation limitée au stockage interne)"
                            rootStatut == true -> "Accès Root accordé (partitions / débloquées)"
                            rootStatut == false -> "Appareil non rooté ou permission refusée"
                            else -> "Vérification en cours..."
                        }
                        Text(
                            text = etatRoot,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = rootActive,
                        onCheckedChange = { actif ->
                            AppPreferences.setRootActive(context, actif)
                            if (actif) {
                                scope.launch { RootManager.verifierEtConnecter(context) }
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
