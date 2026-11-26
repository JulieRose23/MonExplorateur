package com.monexplorateur.app.ui.screens

import android.widget.Toast
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
import com.monexplorateur.app.data.LangueApp
import com.monexplorateur.app.root.RootManager
import com.monexplorateur.app.shizuku.ShizukuManager
import com.monexplorateur.app.ui.theme.ModeTheme
import kotlinx.coroutines.launch

@Composable
fun ReglagesScreen(paddingBasNavigation: PaddingValues = PaddingValues(0.dp)) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val modeTheme by AppPreferences.modeTheme.collectAsState()
    val langue by AppPreferences.langue.collectAsState()
    val rootActive by AppPreferences.rootActive.collectAsState()
    val shizukuActive by AppPreferences.shizukuActive.collectAsState()
    val en = langue == LangueApp.EN

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
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (en) "Appearance & Language" else "Apparence & Langue",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(if (en) "Language" else "Langue", style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LangueApp.entries.forEach { item ->
                        FilterChip(
                            selected = langue == item,
                            onClick = { AppPreferences.setLangue(context, item) },
                            label = { Text(item.libelle) }
                        )
                    }
                }

                HorizontalDivider()

                Text(if (en) "App Theme" else "Thème de l'application", style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeTheme.entries.forEach { mode ->
                        val nomTheme = when (mode) {
                            ModeTheme.CLAIR -> if (en) "Light" else "Clair"
                            ModeTheme.SOMBRE -> if (en) "Dark" else "Sombre"
                            ModeTheme.SYSTEME -> if (en) "System" else "Système"
                        }
                        FilterChip(
                            selected = modeTheme == mode,
                            onClick = { AppPreferences.setModeTheme(context, mode) },
                            label = { Text(nomTheme) }
                        )
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (en) "System Access & Privileges" else "Accès Système & Privilèges",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (en) "Shizuku Mode (/Android/data)" else "Mode Shizuku (/Android/data)",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        val etatShizuku = when {
                            !shizukuActive -> if (en) "Disabled — enable to authorize Shizuku" else "Désactivé — activez pour autoriser Shizuku"
                            shizukuDispo && shizukuPerm -> if (en) "Connected and authorized" else "Connecté et autorisé"
                            shizukuDispo -> if (en) "Service detected — Authorization required" else "Service détecté — Autorisation requise"
                            else -> if (en) "Shizuku service not running" else "Application Shizuku non démarrée"
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
                            if (actif) {
                                val msg = ShizukuManager.demanderPermissionDepuisReglages(context)
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            } else {
                                ShizukuManager.deconnecter()
                            }
                        }
                    )
                }

                HorizontalDivider()

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (en) "Superuser Access (Root / su)" else "Accès Superutilisateur (Root / su)",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        val etatRoot = when {
                            !rootActive -> if (en) "Disabled (limited to internal storage)" else "Désactivé (navigation limitée au stockage interne)"
                            rootStatut == true -> if (en) "Root access granted (/ partitions unlocked)" else "Accès Root accordé (partitions / débloquées)"
                            rootStatut == false -> if (en) "Device not rooted or permission denied" else "Appareil non rooté ou permission refusée"
                            else -> if (en) "Checking..." else "Vérification en cours..."
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
