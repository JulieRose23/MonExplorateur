package com.monexplorateur.app.ui.screens

import android.content.Context
import android.content.Intent
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.monexplorateur.app.root.FichierRoot
import com.monexplorateur.app.root.RootManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class PressePapierRoot(
    val fichier: FichierRoot,
    val couper: Boolean
)

@Composable
fun ExplorateurRootScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val aAccesRoot by RootManager.aAccesRoot.collectAsState()
    val serviceConnecte by RootManager.serviceConnecte.collectAsState()

    LaunchedEffect(Unit) {
        RootManager.verifierEtConnecter(context)
    }

    if (aAccesRoot == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Demande d'accès superutilisateur (su)...")
            }
        }
        return
    }

    if (aAccesRoot == false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Accès Root non disponible",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Cet appareil n'est pas rooté ou la permission superutilisateur (Magisk / KernelSU) a été refusée.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = {
                scope.launch { RootManager.verifierEtConnecter(context) }
            }) {
                Text("Réessayer")
            }
        }
        return
    }

    if (!serviceConnecte) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Démarrage du service Root...")
            }
        }
        return
    }

    var cheminCourant by remember { mutableStateOf("/") }
    var fichiers by remember { mutableStateOf<List<FichierRoot>>(emptyList()) }
    var chargement by remember { mutableStateOf(true) }
    var compteurRefresh by remember { mutableIntStateOf(0) }

    var pressePapier by remember { mutableStateOf<PressePapierRoot?>(null) }
    var dialogueNouveauDossier by remember { mutableStateOf(false) }
    var fichierARenommer by remember { mutableStateOf<FichierRoot?>(null) }
    var fichierASupprimer by remember { mutableStateOf<FichierRoot?>(null) }

    LaunchedEffect(cheminCourant, compteurRefresh) {
        chargement = true
        fichiers = withContext(Dispatchers.IO) {
            RootManager.listerFichiers(cheminCourant)
        }
        chargement = false
    }

    BackHandler(enabled = cheminCourant != "/") {
        val parent = File(cheminCourant).parent
        cheminCourant = if (parent.isNullOrEmpty()) "/" else parent
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { dialogueNouveauDossier = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nouveau dossier")
            }
        },
        bottomBar = {
            pressePapier?.let { item ->
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (item.couper) "Déplacer (Root) :" else "Copier (Root) :",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = item.fichier.nom,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Button(onClick = {
                            scope.launch {
                                val dest = if (cheminCourant == "/") "/${item.fichier.nom}" else "$cheminCourant/${item.fichier.nom}"
                                val ok = withContext(Dispatchers.IO) {
                                    if (item.couper) {
                                        RootManager.renommer(item.fichier.cheminComplet, dest) ||
                                            (RootManager.copier(item.fichier.cheminComplet, dest) &&
                                                RootManager.supprimer(item.fichier.cheminComplet))
                                    } else {
                                        RootManager.copier(item.fichier.cheminComplet, dest)
                                    }
                                }
                                if (ok) {
                                    Toast.makeText(context, "Opération Root réussie", Toast.LENGTH_SHORT).show()
                                    if (item.couper) pressePapier = null
                                    compteurRefresh++
                                } else {
                                    Toast.makeText(context, "Échec (partition en lecture seule ?)", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Text("Coller ici")
                        }
                        IconButton(onClick = { pressePapier = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Annuler")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    if (cheminCourant != "/") {
                        IconButton(onClick = {
                            val parent = File(cheminCourant).parent
                            cheminCourant = if (parent.isNullOrEmpty()) "/" else parent
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Text(
                        text = cheminCourant,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { compteurRefresh++ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualiser")
                    }
                }
            }
            HorizontalDivider()

            when {
                chargement -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                fichiers.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Dossier vide", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(fichiers, key = { it.cheminComplet }) { item ->
                            LigneFichierRoot(
                                item = item,
                                onClick = {
                                    if (item.estDossier) {
                                        cheminCourant = item.cheminComplet
                                    } else {
                                        scope.launch {
                                            ouvrirViaCacheRoot(context, item)
                                        }
                                    }
                                },
                                onCopier = { pressePapier = PressePapierRoot(item, couper = false) },
                                onCouper = { pressePapier = PressePapierRoot(item, couper = true) },
                                onRenommer = { fichierARenommer = item },
                                onSupprimer = { fichierASupprimer = item }
                            )
                            HorizontalDivider(thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }

    if (dialogueNouveauDossier) {
        var nom by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { dialogueNouveauDossier = false },
            title = { Text("Nouveau dossier (Root)") },
            text = {
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it },
                    label = { Text("Nom du dossier") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val propre = nom.trim()
                    if (propre.isNotEmpty()) {
                        scope.launch {
                            val cible = if (cheminCourant == "/") "/$propre" else "$cheminCourant/$propre"
                            val ok = withContext(Dispatchers.IO) {
                                RootManager.creerDossier(cible)
                            }
                            if (ok) compteurRefresh++
                            else Toast.makeText(context, "Création impossible", Toast.LENGTH_SHORT).show()
                        }
                    }
                    dialogueNouveauDossier = false
                }) { Text("Créer") }
            },
            dismissButton = {
                TextButton(onClick = { dialogueNouveauDossier = false }) { Text("Annuler") }
            }
        )
    }

    fichierARenommer?.let { cible ->
        var nouveauNom by remember { mutableStateOf(cible.nom) }
        AlertDialog(
            onDismissRequest = { fichierARenommer = null },
            title = { Text("Renommer (Root)") },
            text = {
                OutlinedTextField(
                    value = nouveauNom,
                    onValueChange = { nouveauNom = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val propre = nouveauNom.trim()
                    if (propre.isNotEmpty() && propre != cible.nom) {
                        scope.launch {
                            val parent = File(cible.cheminComplet).parent ?: "/"
                            val dest = if (parent == "/") "/$propre" else "$parent/$propre"
                            val ok = withContext(Dispatchers.IO) {
                                RootManager.renommer(cible.cheminComplet, dest)
                            }
                            if (ok) compteurRefresh++
                            else Toast.makeText(context, "Échec du renommage", Toast.LENGTH_SHORT).show()
                        }
                    }
                    fichierARenommer = null
                }) { Text("Valider") }
            },
            dismissButton = {
                TextButton(onClick = { fichierARenommer = null }) { Text("Annuler") }
            }
        )
    }

    fichierASupprimer?.let { cible ->
        AlertDialog(
            onDismissRequest = { fichierASupprimer = null },
            title = { Text("Supprimer en mode Root ?") },
            text = { Text("Attention : supprimer définitivement « ${cible.nom} » ?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            RootManager.supprimer(cible.cheminComplet)
                        }
                        if (ok) {
                            compteurRefresh++
                            Toast.makeText(context, "Supprimé", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Échec de la suppression", Toast.LENGTH_SHORT).show()
                        }
                    }
                    fichierASupprimer = null
                }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { fichierASupprimer = null }) { Text("Annuler") }
            }
        )
    }
}

@Composable
private fun LigneFichierRoot(
    item: FichierRoot,
    onClick: () -> Unit,
    onCopier: () -> Unit,
    onCouper: () -> Unit,
    onRenommer: () -> Unit,
    onSupprimer: () -> Unit
) {
    var menuOuvert by remember { mutableStateOf(false) }
    val dateFormatee = remember(item.derniereModif) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(item.derniereModif))
    }
    val sousTitre = if (item.estDossier) {
        "${item.tailleOuNombre} élément(s) - $dateFormatee"
    } else {
        "${formaterTailleRoot(item.tailleOuNombre)} - $dateFormatee"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp)
    ) {
        Icon(
            imageVector = if (item.estDossier) Icons.AutoMirrored.Filled.List else Icons.Default.Info,
            contentDescription = null,
            tint = if (item.estDossier) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.nom,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = sousTitre,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box {
            IconButton(onClick = { menuOuvert = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Options")
            }
            DropdownMenu(expanded = menuOuvert, onDismissRequest = { menuOuvert = false }) {
                DropdownMenuItem(text = { Text("Copier") }, onClick = { menuOuvert = false; onCopier() })
                DropdownMenuItem(text = { Text("Couper (Déplacer)") }, onClick = { menuOuvert = false; onCouper() })
                DropdownMenuItem(text = { Text("Renommer") }, onClick = { menuOuvert = false; onRenommer() })
                DropdownMenuItem(
                    text = { Text("Supprimer", color = MaterialTheme.colorScheme.error) },
                    onClick = { menuOuvert = false; onSupprimer() }
                )
            }
        }
    }
}

private suspend fun ouvrirViaCacheRoot(context: Context, item: FichierRoot) {
    Toast.makeText(context, "Extraction temporaire (Root)...", Toast.LENGTH_SHORT).show()
    val fichierCache = File(context.cacheDir, "root_preview/${item.nom}")
    val ok = withContext(Dispatchers.IO) {
        RootManager.copierVersCache(item.cheminComplet, fichierCache.absolutePath)
    }
    if (!ok || !fichierCache.exists()) {
        Toast.makeText(context, "Impossible de lire ce fichier", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", fichierCache)
        val ext = fichierCache.extension.lowercase(Locale.getDefault())
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Ouvrir avec"))
    } catch (e: Exception) {
        Toast.makeText(context, "Aucune application compatible", Toast.LENGTH_SHORT).show()
    }
}

private fun formaterTailleRoot(octets: Long): String {
    if (octets < 1024) return "$octets o"
    val ko = octets / 1024.0
    if (ko < 1024) return String.format(Locale.getDefault(), "%.1f Ko", ko)
    val mo = ko / 1024.0
    if (mo < 1024) return String.format(Locale.getDefault(), "%.1f Mo", mo)
    val go = mo / 1024.0
    return String.format(Locale.getDefault(), "%.2f Go", go)
}
