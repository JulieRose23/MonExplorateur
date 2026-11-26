package com.monexplorateur.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class PressePapier(
    val fichier: File,
    val couper: Boolean
)

@Composable
fun ExplorateurStandardScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    fun verifierPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    var aPermission by remember { mutableStateOf(verifierPermission()) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                aPermission = verifierPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        aPermission = verifierPermission()
    }

    if (!aPermission) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Accès au stockage requis",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Autorise l'application à gérer tous les fichiers pour parcourir la mémoire interne.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                    }
                } else {
                    legacyPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    )
                }
            }) {
                Text("Accorder la permission")
            }
        }
        return
    }

    val racine = remember { Environment.getExternalStorageDirectory() }
    var dossierCourant by remember { mutableStateOf(racine) }
    var compteurRafraichissement by remember { mutableIntStateOf(0) }

    // États pour les opérations sur les fichiers
    var pressePapier by remember { mutableStateOf<PressePapier?>(null) }
    var afficherDialogueNouveauDossier by remember { mutableStateOf(false) }
    var fichierARenommer by remember { mutableStateOf<File?>(null) }
    var fichierASupprimer by remember { mutableStateOf<File?>(null) }

    val fichiers = remember(dossierCourant, compteurRafraichissement) {
        dossierCourant.listFiles()
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.getDefault()) }))
            ?: emptyList()
    }

    BackHandler(enabled = dossierCourant.absolutePath != racine.absolutePath) {
        dossierCourant.parentFile?.let { dossierCourant = it }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { afficherDialogueNouveauDossier = true }) {
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
                                text = if (item.couper) "Déplacer :" else "Copier :",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = item.fichier.name,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Button(onClick = {
                            val destination = File(dossierCourant, item.fichier.name)
                            if (destination.exists()) {
                                Toast.makeText(context, "Un élément porte déjà ce nom ici", Toast.LENGTH_SHORT).show()
                            } else {
                                val succes = try {
                                    if (item.couper) {
                                        if (item.fichier.renameTo(destination)) {
                                            true
                                        } else {
                                            item.fichier.copyRecursively(destination, overwrite = false) && item.fichier.deleteRecursively()
                                        }
                                    } else {
                                        item.fichier.copyRecursively(destination, overwrite = false)
                                    }
                                } catch (e: Exception) {
                                    false
                                }
                                if (succes) {
                                    Toast.makeText(context, "Opération réussie", Toast.LENGTH_SHORT).show()
                                    if (item.couper) pressePapier = null
                                    compteurRafraichissement++
                                } else {
                                    Toast.makeText(context, "Échec de l'opération", Toast.LENGTH_SHORT).show()
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
                    if (dossierCourant.absolutePath != racine.absolutePath) {
                        IconButton(onClick = {
                            dossierCourant.parentFile?.let { dossierCourant = it }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Dossier parent"
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Text(
                        text = dossierCourant.absolutePath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            HorizontalDivider()

            if (fichiers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ce dossier est vide",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(fichiers, key = { it.absolutePath }) { fichier ->
                        LigneFichier(
                            fichier = fichier,
                            onClick = {
                                if (fichier.isDirectory) {
                                    dossierCourant = fichier
                                } else {
                                    ouvrirFichier(context, fichier)
                                }
                            },
                            onCopier = { pressePapier = PressePapier(fichier, couper = false) },
                            onCouper = { pressePapier = PressePapier(fichier, couper = true) },
                            onRenommer = { fichierARenommer = fichier },
                            onSupprimer = { fichierASupprimer = fichier }
                        )
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            }
        }
    }

    // Boîte de dialogue : Nouveau dossier
    if (afficherDialogueNouveauDossier) {
        var nomDossier by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { afficherDialogueNouveauDossier = false },
            title = { Text("Nouveau dossier") },
            text = {
                OutlinedTextField(
                    value = nomDossier,
                    onValueChange = { nomDossier = it },
                    label = { Text("Nom du dossier") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val propre = nomDossier.trim()
                        if (propre.isNotEmpty()) {
                            val nouveau = File(dossierCourant, propre)
                            if (nouveau.mkdirs()) {
                                compteurRafraichissement++
                            } else {
                                Toast.makeText(context, "Impossible de créer le dossier", Toast.LENGTH_SHORT).show()
                            }
                        }
                        afficherDialogueNouveauDossier = false
                    }
                ) {
                    Text("Créer")
                }
            },
            dismissButton = {
                TextButton(onClick = { afficherDialogueNouveauDossier = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Boîte de dialogue : Renommer
    fichierARenommer?.let { cible ->
        var nouveauNom by remember { mutableStateOf(cible.name) }
        AlertDialog(
            onDismissRequest = { fichierARenommer = null },
            title = { Text("Renommer") },
            text = {
                OutlinedTextField(
                    value = nouveauNom,
                    onValueChange = { nouveauNom = it },
                    label = { Text("Nouveau nom") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val propre = nouveauNom.trim()
                        if (propre.isNotEmpty() && propre != cible.name) {
                            val dest = File(cible.parentFile, propre)
                            if (cible.renameTo(dest)) {
                                compteurRafraichissement++
                            } else {
                                Toast.makeText(context, "Échec du renommage", Toast.LENGTH_SHORT).show()
                            }
                        }
                        fichierARenommer = null
                    }
                ) {
                    Text("Valider")
                }
            },
            dismissButton = {
                TextButton(onClick = { fichierARenommer = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Boîte de dialogue : Confirmation de suppression
    fichierASupprimer?.let { cible ->
        AlertDialog(
            onDismissRequest = { fichierASupprimer = null },
            title = { Text("Supprimer définitivement ?") },
            text = { Text("Voulez-vous vraiment supprimer « ${cible.name} » ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ok = if (cible.isDirectory) cible.deleteRecursively() else cible.delete()
                        if (ok) {
                            compteurRafraichissement++
                            Toast.makeText(context, "Élément supprimé", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Impossible de supprimer", Toast.LENGTH_SHORT).show()
                        }
                        fichierASupprimer = null
                    }
                ) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { fichierASupprimer = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun LigneFichier(
    fichier: File,
    onClick: () -> Unit,
    onCopier: () -> Unit,
    onCouper: () -> Unit,
    onRenommer: () -> Unit,
    onSupprimer: () -> Unit
) {
    var menuOuvert by remember { mutableStateOf(false) }

    val dateFormatee = remember(fichier.lastModified()) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(fichier.lastModified()))
    }
    val sousTitre = if (fichier.isDirectory) {
        val nbElements = fichier.listFiles()?.size ?: 0
        "$nbElements élément(s) - $dateFormatee"
    } else {
        "${formaterTaille(fichier.length())} - $dateFormatee"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp)
    ) {
        Icon(
            imageVector = if (fichier.isDirectory) Icons.AutoMirrored.Filled.List else Icons.Default.Info,
            contentDescription = null,
            tint = if (fichier.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fichier.name,
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
            DropdownMenu(
                expanded = menuOuvert,
                onDismissRequest = { menuOuvert = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Copier") },
                    onClick = {
                        menuOuvert = false
                        onCopier()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Couper (Déplacer)") },
                    onClick = {
                        menuOuvert = false
                        onCouper()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Renommer") },
                    onClick = {
                        menuOuvert = false
                        onRenommer()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Supprimer", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        menuOuvert = false
                        onSupprimer()
                    }
                )
            }
        }
    }
}

private fun ouvrirFichier(context: Context, fichier: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            fichier
        )
        val extension = fichier.extension.lowercase(Locale.getDefault())
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Ouvrir avec"))
    } catch (e: Exception) {
        Toast.makeText(context, "Aucune application trouvée pour ouvrir ce fichier", Toast.LENGTH_SHORT).show()
    }
}

private fun formaterTaille(octets: Long): String {
    if (octets < 1024) return "$octets o"
    val ko = octets / 1024.0
    if (ko < 1024) return String.format(Locale.getDefault(), "%.1f Ko", ko)
    val mo = ko / 1024.0
    if (mo < 1024) return String.format(Locale.getDefault(), "%.1f Mo", mo)
    val go = mo / 1024.0
    return String.format(Locale.getDefault(), "%.2f Go", go)
}
