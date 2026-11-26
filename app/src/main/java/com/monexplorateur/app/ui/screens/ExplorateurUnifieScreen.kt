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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.monexplorateur.app.data.AppPreferences
import com.monexplorateur.app.data.ElementFichier
import com.monexplorateur.app.data.LangueApp
import com.monexplorateur.app.data.SourceFichier
import com.monexplorateur.app.data.TrashManager
import com.monexplorateur.app.data.UnifiedFileEngine
import com.monexplorateur.app.notifications.ArchiveNotificationHelper
import com.monexplorateur.app.root.RootManager
import com.monexplorateur.app.shizuku.ShizukuManager
import com.monexplorateur.app.ui.components.BadgeIconeElement
import com.monexplorateur.app.ui.components.IconeDossierModerne
import com.monexplorateur.app.utils.FileUtils
import com.monexplorateur.app.utils.FormatArchive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val FORMATEUR_DATE = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

private data class PressePapierMulti(
    val elements: List<ElementFichier>,
    val couper: Boolean
)

@Composable
fun ExplorateurUnifieScreen(paddingBasNavigation: PaddingValues = PaddingValues(0.dp)) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

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
    val rootActive by AppPreferences.rootActive.collectAsState()
    val langue by AppPreferences.langue.collectAsState()
    val en = langue == LangueApp.EN
    val shizukuActive by AppPreferences.shizukuActive.collectAsState()
    val rootConnecte by RootManager.serviceConnecte.collectAsState()
    val shizukuConnecte by ShizukuManager.serviceConnecte.collectAsState()

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
    ) { aPermission = verifierPermission() }

    if (!aPermission) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(if (en) "Storage access required" else "Accès au stockage requis", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (en) "Allow full file access to browse your device." else "Autorise l'accès complet aux fichiers pour explorer le téléphone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:${context.packageName}")
                        })
                    } catch (_: Exception) {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                    }
                } else {
                    legacyPermissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE))
                }
            }) { Text(if (en) "Grant permission" else "Accorder la permission") }
        }
        return
    }

    val racineStockage = remember { UnifiedFileEngine.racineStockage }
    var cheminCourant by remember { mutableStateOf(racineStockage) }
    var fichiers by remember { mutableStateOf<List<ElementFichier>>(emptyList()) }
    var chargement by remember { mutableStateOf(true) }
    var compteurRefresh by remember { mutableIntStateOf(0) }

    var selection by remember { mutableStateOf<Set<String>>(emptySet()) }
    val enModeSelection = selection.isNotEmpty()

    var rechercheActive by remember { mutableStateOf(false) }
    var requeteRecherche by remember { mutableStateOf("") }
    var resultatsRecherche by remember { mutableStateOf<List<ElementFichier>>(emptyList()) }
    var rechercheEnCours by remember { mutableStateOf(false) }

    var pressePapier by remember { mutableStateOf<PressePapierMulti?>(null) }
    var dialogueNouveauDossier by remember { mutableStateOf(false) }
    var elementARenommer by remember { mutableStateOf<ElementFichier?>(null) }
    var elementsASupprimer by remember { mutableStateOf<List<ElementFichier>>(emptyList()) }
    var elementProprietes by remember { mutableStateOf<ElementFichier?>(null) }

    var archiveAExtraire by remember { mutableStateOf<ElementFichier?>(null) }
    var archiveDestinationPicker by remember { mutableStateOf<ElementFichier?>(null) }
    var elementsACompresser by remember { mutableStateOf<List<ElementFichier>>(emptyList()) }

    LaunchedEffect(cheminCourant, compteurRefresh, rootConnecte, shizukuConnecte, rootActive, shizukuActive) {
        chargement = true
        fichiers = UnifiedFileEngine.listerDossier(cheminCourant).filterNot { it.nom == ".MonExplorateur_Corbeille" }
        chargement = false
    }

    LaunchedEffect(requeteRecherche, rechercheActive) {
        if (!rechercheActive || requeteRecherche.trim().length < 2) {
            resultatsRecherche = emptyList()
            rechercheEnCours = false
            return@LaunchedEffect
        }
        rechercheEnCours = true
        delay(350)
        resultatsRecherche = UnifiedFileEngine.rechercherPartout(requeteRecherche)
        rechercheEnCours = false
    }

    BackHandler(enabled = enModeSelection || rechercheActive || (cheminCourant != "/" && cheminCourant != racineStockage)) {
        when {
            enModeSelection -> selection = emptySet()
            rechercheActive -> {
                rechercheActive = false
                requeteRecherche = ""
            }
            else -> {
                val parent = File(cheminCourant).parent
                cheminCourant = when {
                    parent.isNullOrEmpty() -> "/"
                    !rootActive && !parent.startsWith(racineStockage) -> racineStockage
                    else -> parent
                }
            }
        }
    }

    val listeAffichee = if (rechercheActive && requeteRecherche.trim().length >= 2) resultatsRecherche else fichiers

    Scaffold(
        modifier = Modifier.padding(paddingBasNavigation),
        floatingActionButton = {
            if (!enModeSelection && !rechercheActive) {
                FloatingActionButton(onClick = { dialogueNouveauDossier = true }) {
                    Icon(Icons.Default.Add, contentDescription = if (en) "New folder" else "Nouveau dossier")
                }
            }
        },
        bottomBar = {
            pressePapier?.let { pp ->
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (pp.couper) {
                                    if (en) "Move ${pp.elements.size} item(s):" else "Déplacer ${pp.elements.size} élément(s) :"
                                } else {
                                    if (en) "Copy ${pp.elements.size} item(s):" else "Copier ${pp.elements.size} élément(s) :"
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = pp.elements.joinToString(", ") { it.nom },
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Button(onClick = {
                            scope.launch {
                                Toast.makeText(context, if (en) "Processing..." else "Traitement en cours...", Toast.LENGTH_SHORT).show()
                                val ok = UnifiedFileEngine.copierOuDeplacer(pp.elements, cheminCourant, pp.couper)
                                Toast.makeText(
                                    context,
                                    if (ok) (if (en) "Operation completed" else "Opération terminée")
                                    else (if (en) "Some items could not be processed" else "Certains éléments n'ont pas pu être traités"),
                                    Toast.LENGTH_SHORT
                                ).show()
                                if (pp.couper) pressePapier = null
                                compteurRefresh++
                            }
                        }) { Text(if (en) "Paste here" else "Coller ici") }
                        IconButton(onClick = { pressePapier = null }) {
                            Icon(Icons.Default.Close, contentDescription = if (en) "Cancel" else "Annuler")
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
                color = if (enModeSelection) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                when {
                    enModeSelection -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            IconButton(onClick = { selection = emptySet() }) {
                                Icon(Icons.Default.Close, contentDescription = if (en) "Exit" else "Quitter")
                            }
                            Text(
                                text = "${selection.size}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = {
                                selection = if (selection.size == listeAffichee.size) emptySet()
                                else listeAffichee.map { it.cheminComplet }.toSet()
                            }) { Text(if (en) "All" else "Tout") }
                            TextButton(onClick = {
                                elementsACompresser = listeAffichee.filter { it.cheminComplet in selection }
                            }) { Text(if (en) "Archive" else "Archiver") }
                            TextButton(onClick = {
                                val choisis = listeAffichee.filter { it.cheminComplet in selection }
                                pressePapier = PressePapierMulti(choisis, couper = true)
                                selection = emptySet()
                            }) { Text(if (en) "Move" else "Déplacer") }
                            IconButton(onClick = {
                                elementsASupprimer = listeAffichee.filter { it.cheminComplet in selection }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = if (en) "Delete" else "Supprimer", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    rechercheActive -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            OutlinedTextField(
                                value = requeteRecherche,
                                onValueChange = { requeteRecherche = it },
                                placeholder = { Text(if (en) "Search everywhere..." else "Rechercher partout...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                rechercheActive = false
                                requeteRecherche = ""
                            }) {
                                Icon(Icons.Default.Close, contentDescription = if (en) "Close" else "Fermer")
                            }
                        }
                    }
                    else -> {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                val peutRemonter = cheminCourant != "/" && (rootActive || cheminCourant != racineStockage)
                                if (peutRemonter) {
                                    IconButton(onClick = {
                                        val parent = File(cheminCourant).parent
                                        cheminCourant = if (parent.isNullOrEmpty()) "/" else parent
                                    }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = if (en) "Back" else "Retour")
                                    }
                                } else {
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                Text(
                                    text = cheminCourant,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { rechercheActive = true }) {
                                    Icon(Icons.Default.Search, contentDescription = if (en) "Search everywhere" else "Rechercher partout")
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                AssistChip(
                                    onClick = { cheminCourant = racineStockage },
                                    label = { Text(if (en) "Internal Storage" else "Mémoire interne") }
                                )
                                if (shizukuActive) {
                                    AssistChip(
                                        onClick = { cheminCourant = "$racineStockage/Android/data" },
                                        label = { Text("Android/data (Shizuku)") }
                                    )
                                }
                                if (rootActive) {
                                    AssistChip(
                                        onClick = { cheminCourant = "/" },
                                        label = { Text(if (en) "Root /" else "Racine / (Root)") }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            HorizontalDivider()

            when {
                chargement || rechercheEnCours -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            if (rechercheEnCours) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(if (en) "Searching everywhere..." else "Recherche globale en cours...")
                            }
                        }
                    }
                }
                listeAffichee.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (rechercheActive) {
                                if (en) "No files found" else "Aucun fichier trouvé"
                            } else if (en) "This folder is empty" else "Ce dossier est vide",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 84.dp)
                    ) {
                        items(listeAffichee, key = { it.cheminComplet }) { item ->
                            val estSelectionne = item.cheminComplet in selection
                            LigneElementUnifie(
                                item = item,
                                en = en,
                                estSelectionne = estSelectionne,
                                afficherCheminComplet = rechercheActive,
                                onClick = {
                                    if (enModeSelection) {
                                        selection = if (estSelectionne) selection - item.cheminComplet else selection + item.cheminComplet
                                    } else if (item.estDossier) {
                                        rechercheActive = false
                                        requeteRecherche = ""
                                        cheminCourant = item.cheminComplet
                                    } else if (FileUtils.estArchiveSupportee(item.nom)) {
                                        archiveAExtraire = item
                                    } else {
                                        scope.launch { ouvrirElement(context, item, en) }
                                    }
                                },
                                onLongClick = {
                                    selection = if (estSelectionne) selection - item.cheminComplet else selection + item.cheminComplet
                                },
                                onCopier = { pressePapier = PressePapierMulti(listOf(item), couper = false) },
                                onCouper = { pressePapier = PressePapierMulti(listOf(item), couper = true) },
                                onRenommer = { elementARenommer = item },
                                onCompresser = { elementsACompresser = listOf(item) },
                                onExtraire = { archiveAExtraire = item },
                                onProprietes = { elementProprietes = item },
                                onSupprimer = { elementsASupprimer = listOf(item) }
                            )
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    }

    // Boîte de choix de format pour compresser (ZIP, 7Z, TAR.GZ)
    if (elementsACompresser.isNotEmpty()) {
        val cibles = elementsACompresser
        val nomDefaut = if (cibles.size == 1) File(cibles.first().nom).nameWithoutExtension else "Archive"
        var nomArchive by remember { mutableStateOf(nomDefaut) }
        var formatChoisi by remember { mutableStateOf(FormatArchive.ZIP) }

        AlertDialog(
            onDismissRequest = { elementsACompresser = emptyList() },
            title = {
                val libelleNb = if (en) {
                    if (cibles.size > 1) "${cibles.size} items" else "1 item"
                } else {
                    if (cibles.size > 1) "${cibles.size} éléments" else "1 élément"
                }
                Text(
                    if (en) "Create archive ($libelleNb)"
                    else "Créer une archive ($libelleNb)"
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nomArchive,
                        onValueChange = { nomArchive = it },
                        label = { Text(if (en) "Archive name" else "Nom de l'archive") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        if (en) "Compression format:" else "Format de compression :",
                        style = MaterialTheme.typography.labelLarge
                    )
                    FormatArchive.entries.forEach { fmt ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { formatChoisi = fmt }
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(selected = formatChoisi == fmt, onClick = { formatChoisi = fmt })
                            Text(fmt.libelle(en), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val propre = nomArchive.trim().ifEmpty { "Archive" }
                    val fmt = formatChoisi
                    elementsACompresser = emptyList()
                    selection = emptySet()
                    scope.launch {
                        val dest = File(cheminCourant, "$propre.${fmt.extension}")
                        val notifHelper = ArchiveNotificationHelper(context)
                        val notifId = (System.currentTimeMillis() and 0x7FFFFFFF).toInt()
                        val titreNotif = if (en) "Compressing: ${dest.name}" else "Compression : ${dest.name}"

                        Toast.makeText(
                            context,
                            if (en) "Compressing ${fmt.extension.uppercase()}..." else "Compression ${fmt.extension.uppercase()} en cours...",
                            Toast.LENGTH_SHORT
                        ).show()
                        notifHelper.showProgress(
                            title = titreNotif,
                            text = if (en) "Preparing files..." else "Préparation des fichiers...",
                            progress = 0,
                            indeterminate = true,
                            notifId = notifId
                        )

                        val fichiersLocaux = cibles.mapNotNull { UnifiedFileEngine.preparerFichierLocal(context, it) }
                        val ok = withContext(Dispatchers.IO) {
                            FileUtils.compresser(fichiersLocaux, dest, fmt) { pct ->
                                notifHelper.showProgress(
                                    title = titreNotif,
                                    text = "$pct %",
                                    progress = pct,
                                    indeterminate = false,
                                    notifId = notifId
                                )
                            }
                        }
                        if (ok) {
                            compteurRefresh++
                            notifHelper.showCompleted(
                                title = if (en) "Archive created: ${dest.name}" else "Archive créée : ${dest.name}",
                                text = if (en) "Compression completed" else "Compression terminée avec succès",
                                success = true,
                                notifId = notifId
                            )
                            Toast.makeText(
                                context,
                                if (en) "Archive created: ${dest.name}" else "Archive créée : ${dest.name}",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            notifHelper.showCompleted(
                                title = if (en) "Compression failed: ${dest.name}" else "Échec de la compression : ${dest.name}",
                                text = if (en) "An error occurred" else "Une erreur est survenue",
                                success = false,
                                notifId = notifId
                            )
                            Toast.makeText(
                                context,
                                if (en) "Compression failed" else "Échec de la compression",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }) { Text(if (en) "Compress" else "Compresser") }
            },
            dismissButton = {
                TextButton(onClick = { elementsACompresser = emptyList() }) { Text(if (en) "Cancel" else "Annuler") }
            }
        )
    }

    // Boîte de choix au clic sur une archive (ZIP, 7Z, RAR, TAR.GZ...)
    archiveAExtraire?.let { archiveItem ->
        AlertDialog(
            onDismissRequest = { archiveAExtraire = null },
            title = { Text("Archive : ${archiveItem.nom}") },
            text = {
                Text(
                    if (en) "How would you like to handle this archive?"
                    else "Comment souhaitez-vous traiter cette archive ?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    archiveAExtraire = null
                    archiveDestinationPicker = archiveItem
                }) { Text(if (en) "Choose destination..." else "Choisir où extraire...") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        archiveAExtraire = null
                        scope.launch { ouvrirElement(context, archiveItem, en) }
                    }) { Text(if (en) "Open" else "Ouvrir") }
                    TextButton(onClick = {
                        val cible = archiveItem
                        archiveAExtraire = null
                        scope.launch {
                            extraireArchiveVers(context, cible, cheminCourant, en) { compteurRefresh++ }
                        }
                    }) { Text(if (en) "Extract here" else "Extraire ici") }
                }
            }
        )
    }

    // Mini-navigateur pour choisir le dossier d'extraction
    archiveDestinationPicker?.let { archiveItem ->
        var dossierCible by remember { mutableStateOf(cheminCourant) }
        var sousDossiers by remember { mutableStateOf<List<ElementFichier>>(emptyList()) }

        LaunchedEffect(dossierCible) {
            sousDossiers = UnifiedFileEngine.listerDossier(dossierCible).filter { it.estDossier }
        }

        AlertDialog(
            onDismissRequest = { archiveDestinationPicker = null },
            title = { Text(if (en) "Extract to..." else "Extraire vers...") },
            text = {
                Column(modifier = Modifier.height(280.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (dossierCible != "/" && dossierCible != racineStockage) {
                            IconButton(onClick = {
                                dossierCible = File(dossierCible).parent ?: racineStockage
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Parent")
                            }
                        }
                        Text(
                            text = dossierCible,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    HorizontalDivider()
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(sousDossiers, key = { it.cheminComplet }) { d ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { dossierCible = d.cheminComplet }
                                    .padding(vertical = 10.dp, horizontal = 8.dp)
                            ) {
                                Icon(IconeDossierModerne, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(d.nom, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val dest = dossierCible
                    val cible = archiveItem
                    archiveDestinationPicker = null
                    scope.launch {
                        extraireArchiveVers(context, cible, dest, en) { compteurRefresh++ }
                    }
                }) { Text(if (en) "Extract here" else "Extraire ici") }
            },
            dismissButton = {
                TextButton(onClick = { archiveDestinationPicker = null }) { Text(if (en) "Cancel" else "Annuler") }
            }
        )
    }

    // Dialogue Propriétés + MD5 / SHA-256
    elementProprietes?.let { cible ->
        var md5 by remember { mutableStateOf<String?>(null) }
        var sha256 by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(cible) {
            val local = UnifiedFileEngine.preparerFichierLocal(context, cible)
            withContext(Dispatchers.IO) {
                if (local != null && local.isFile) {
                    md5 = FileUtils.calculerHash(local, "MD5")
                    sha256 = FileUtils.calculerHash(local, "SHA-256")
                } else {
                    md5 = "N/A"
                    sha256 = "N/A"
                }
            }
        }

        val dateTxt = remember(cible) { FORMATEUR_DATE.format(Date(cible.derniereModif)) }
        val libelleType = when {
            cible.estDossier -> if (en) "Folder" else "Dossier"
            FileUtils.estArchiveSupportee(cible.nom) -> "Archive"
            else -> if (en) "File" else "Fichier"
        }

        AlertDialog(
            onDismissRequest = { elementProprietes = null },
            title = {
                Text(
                    if (en) "Properties (${cible.source.badge})"
                    else "Propriétés (${cible.source.badge})"
                )
            },
            text = {
                SelectionContainer {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${if (en) "Name:" else "Nom :"} ${cible.nom}", fontWeight = FontWeight.Bold)
                        Text("${if (en) "Path:" else "Chemin :"} ${cible.cheminComplet}", style = MaterialTheme.typography.bodySmall)
                        Text("${if (en) "Type:" else "Type :"} $libelleType", style = MaterialTheme.typography.bodySmall)
                        if (!cible.estDossier) {
                            Text(
                                text = "${if (en) "Size:" else "Taille :"} ${formaterTailleUnifie(cible.tailleOuNombre, en)} (${cible.tailleOuNombre} ${if (en) "bytes" else "octets"})",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text("${if (en) "Modified:" else "Modifié le :"} $dateTxt", style = MaterialTheme.typography.bodySmall)
                        if (!cible.estDossier) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text("MD5 :", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelSmall)
                            Text(md5 ?: if (en) "Calculating..." else "Calcul...", style = MaterialTheme.typography.bodySmall)
                            Text("SHA-256 :", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelSmall)
                            Text(sha256 ?: if (en) "Calculating..." else "Calcul...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { elementProprietes = null }) { Text(if (en) "Close" else "Fermer") }
            }
        )
    }

    // Dialogue Nouveau dossier
    if (dialogueNouveauDossier) {
        var nomDossier by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { dialogueNouveauDossier = false },
            title = { Text(if (en) "New folder" else "Nouveau dossier") },
            text = {
                OutlinedTextField(
                    value = nomDossier,
                    onValueChange = { nomDossier = it },
                    label = { Text(if (en) "Folder name" else "Nom du dossier") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val propre = nomDossier.trim()
                    if (propre.isNotEmpty()) {
                        scope.launch {
                            val cible = if (cheminCourant == "/") "/$propre" else "$cheminCourant/$propre"
                            if (UnifiedFileEngine.creerDossier(cible)) compteurRefresh++
                            else Toast.makeText(context, if (en) "Creation failed" else "Création impossible", Toast.LENGTH_SHORT).show()
                        }
                    }
                    dialogueNouveauDossier = false
                }) { Text(if (en) "Create" else "Créer") }
            },
            dismissButton = {
                TextButton(onClick = { dialogueNouveauDossier = false }) { Text(if (en) "Cancel" else "Annuler") }
            }
        )
    }

    // Dialogue Renommer
    elementARenommer?.let { cible ->
        var nouveauNom by remember { mutableStateOf(cible.nom) }
        AlertDialog(
            onDismissRequest = { elementARenommer = null },
            title = { Text(if (en) "Rename" else "Renommer") },
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
                            if (UnifiedFileEngine.renommer(cible, propre)) compteurRefresh++
                            else Toast.makeText(context, if (en) "Rename failed" else "Échec du renommage", Toast.LENGTH_SHORT).show()
                        }
                    }
                    elementARenommer = null
                }) { Text(if (en) "Confirm" else "Valider") }
            },
            dismissButton = {
                TextButton(onClick = { elementARenommer = null }) { Text(if (en) "Cancel" else "Annuler") }
            }
        )
    }

    // Dialogue Suppression
    if (elementsASupprimer.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { elementsASupprimer = emptyList() },
            title = {
                Text(
                    if (en) "Move to trash (${elementsASupprimer.size})?"
                    else "Mettre à la corbeille (${elementsASupprimer.size}) ?"
                )
            },
            text = {
                Text(
                    if (elementsASupprimer.size == 1) {
                        if (en) "Move « ${elementsASupprimer.first().nom} » to trash?"
                        else "Déplacer « ${elementsASupprimer.first().nom} » vers la corbeille ?"
                    } else {
                        if (en) "Move these ${elementsASupprimer.size} items to trash?"
                        else "Déplacer ces ${elementsASupprimer.size} éléments vers la corbeille ?"
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val aSuppr = elementsASupprimer
                    elementsASupprimer = emptyList()
                    selection = emptySet()
                    scope.launch {
                        var okCount = 0
                        for (el in aSuppr) {
                            if (TrashManager.mettreALaCorbeille(context, el)) okCount++
                        }
                        compteurRefresh++
                        Toast.makeText(
                            context,
                            if (en) "$okCount item(s) moved to trash" else "$okCount élément(s) déplacé(s) dans la corbeille",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }) { Text(if (en) "Trash" else "Corbeille", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { elementsASupprimer = emptyList() }) { Text(if (en) "Cancel" else "Annuler") }
                    TextButton(onClick = {
                        val aSuppr = elementsASupprimer
                        elementsASupprimer = emptyList()
                        selection = emptySet()
                        scope.launch {
                            var okCount = 0
                            for (el in aSuppr) {
                                if (UnifiedFileEngine.supprimer(el)) okCount++
                            }
                            compteurRefresh++
                            Toast.makeText(
                                context,
                                if (en) "$okCount item(s) permanently deleted" else "$okCount élément(s) supprimé(s) définitivement",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }) { Text(if (en) "Permanent" else "Définitif", color = MaterialTheme.colorScheme.error) }
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LigneElementUnifie(
    item: ElementFichier,
    en: Boolean,
    estSelectionne: Boolean,
    afficherCheminComplet: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onCopier: () -> Unit,
    onCouper: () -> Unit,
    onRenommer: () -> Unit,
    onCompresser: () -> Unit,
    onExtraire: () -> Unit,
    onProprietes: () -> Unit,
    onSupprimer: () -> Unit
) {
    var menuOuvert by remember { mutableStateOf(false) }
    val dateFormatee = remember(item.derniereModif) { FORMATEUR_DATE.format(Date(item.derniereModif)) }
    val sousTitre = when {
        afficherCheminComplet -> item.cheminComplet
        item.estDossier -> if (item.tailleOuNombre >= 0) {
            if (en) "${item.tailleOuNombre} item(s) • $dateFormatee" else "${item.tailleOuNombre} élément(s) • $dateFormatee"
        } else {
            "${if (en) "Folder" else "Dossier"} • $dateFormatee"
        }
        else -> "${formaterTailleUnifie(item.tailleOuNombre, en)} • $dateFormatee"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (estSelectionne) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                else MaterialTheme.colorScheme.surface
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 16.dp, end = 4.dp, top = 11.dp, bottom = 11.dp)
    ) {
        BadgeIconeElement(
            nom = item.nom,
            estDossier = item.estDossier,
            estSelectionne = estSelectionne
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.nom,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (item.estDossier) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (item.source != SourceFichier.STANDARD) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "[${item.source.badge}]",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = sousTitre,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box {
            IconButton(onClick = { menuOuvert = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Options")
            }
            DropdownMenu(expanded = menuOuvert, onDismissRequest = { menuOuvert = false }) {
                DropdownMenuItem(text = { Text(if (en) "Select" else "Sélectionner") }, onClick = { menuOuvert = false; onLongClick() })
                DropdownMenuItem(text = { Text(if (en) "Copy" else "Copier") }, onClick = { menuOuvert = false; onCopier() })
                DropdownMenuItem(text = { Text(if (en) "Move" else "Déplacer") }, onClick = { menuOuvert = false; onCouper() })
                DropdownMenuItem(text = { Text(if (en) "Rename" else "Renommer") }, onClick = { menuOuvert = false; onRenommer() })
                if (!item.estDossier && FileUtils.estArchiveSupportee(item.nom)) {
                    DropdownMenuItem(text = { Text(if (en) "Extract..." else "Extraire...") }, onClick = { menuOuvert = false; onExtraire() })
                } else {
                    DropdownMenuItem(text = { Text(if (en) "Compress..." else "Compresser...") }, onClick = { menuOuvert = false; onCompresser() })
                }
                DropdownMenuItem(text = { Text(if (en) "Properties / MD5" else "Propriétés / MD5") }, onClick = { menuOuvert = false; onProprietes() })
                DropdownMenuItem(
                    text = { Text(if (en) "Delete" else "Supprimer", color = MaterialTheme.colorScheme.error) },
                    onClick = { menuOuvert = false; onSupprimer() }
                )
            }
        }
    }
}

private suspend fun extraireArchiveVers(
    context: Context,
    archiveItem: ElementFichier,
    dossierParentDestination: String,
    en: Boolean,
    onSucces: () -> Unit
) {
    val notifHelper = ArchiveNotificationHelper(context)
    val notifId = (System.currentTimeMillis() and 0x7FFFFFFF).toInt()
    val titreNotif = if (en) "Extracting: ${archiveItem.nom}" else "Extraction : ${archiveItem.nom}"

    Toast.makeText(context, if (en) "Extracting..." else "Extraction en cours...", Toast.LENGTH_SHORT).show()
    notifHelper.showProgress(
        title = titreNotif,
        text = if (en) "Preparing archive..." else "Préparation de l'archive...",
        progress = 0,
        indeterminate = true,
        notifId = notifId
    )

    val localArchive = UnifiedFileEngine.preparerFichierLocal(context, archiveItem)
    if (localArchive == null) {
        notifHelper.showCompleted(
            title = if (en) "Extraction failed: ${archiveItem.nom}" else "Échec de l'extraction : ${archiveItem.nom}",
            text = if (en) "Unable to read archive" else "Impossible de lire l'archive",
            success = false,
            notifId = notifId
        )
        Toast.makeText(context, if (en) "Unable to read archive" else "Impossible de lire l'archive", Toast.LENGTH_SHORT).show()
        return
    }
    val nomDossier = FileUtils.nomSansExtensionArchive(archiveItem.nom)
    val dossierSortie = File(dossierParentDestination, nomDossier)
    val ok = withContext(Dispatchers.IO) {
        FileUtils.decompresser(localArchive, dossierSortie) { pct ->
            notifHelper.showProgress(
                title = titreNotif,
                text = "$pct %",
                progress = pct,
                indeterminate = false,
                notifId = notifId
            )
        }
    }
    if (ok) {
        onSucces()
        notifHelper.showCompleted(
            title = if (en) "Extraction completed: ${archiveItem.nom}" else "Extraction terminée : ${archiveItem.nom}",
            text = if (en) "Extracted to $nomDossier/" else "Extrait dans $nomDossier/",
            success = true,
            notifId = notifId
        )
        Toast.makeText(
            context,
            if (en) "Extracted to $nomDossier/" else "Extrait dans $nomDossier/",
            Toast.LENGTH_SHORT
        ).show()
    } else {
        notifHelper.showCompleted(
            title = if (en) "Extraction failed: ${archiveItem.nom}" else "Échec de l'extraction : ${archiveItem.nom}",
            text = if (en) "Encrypted or corrupted archive?" else "Format chiffré ou corrompu ?",
            success = false,
            notifId = notifId
        )
        Toast.makeText(
            context,
            if (en) "Extraction failed (encrypted or corrupted?)" else "Échec de l'extraction (format chiffré ou corrompu ?)",
            Toast.LENGTH_LONG
        ).show()
    }
}

private suspend fun ouvrirElement(context: Context, item: ElementFichier, en: Boolean) {
    val fichierLocal = UnifiedFileEngine.preparerFichierLocal(context, item)
    if (fichierLocal == null || !fichierLocal.exists()) {
        Toast.makeText(context, if (en) "Unable to prepare file" else "Impossible de préparer ce fichier", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", fichierLocal)
        val ext = fichierLocal.extension.lowercase(Locale.getDefault())
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, if (en) "Open with" else "Ouvrir avec"))
    } catch (_: Exception) {
        Toast.makeText(context, if (en) "No compatible app found" else "Aucune application compatible", Toast.LENGTH_SHORT).show()
    }
}

private fun formaterTailleUnifie(octets: Long, en: Boolean): String {
    if (octets < 1024) return if (en) "$octets B" else "$octets o"
    val ko = octets / 1024.0
    if (ko < 1024) return String.format(Locale.getDefault(), if (en) "%.1f KB" else "%.1f Ko", ko)
    val mo = ko / 1024.0
    if (mo < 1024) return String.format(Locale.getDefault(), if (en) "%.1f MB" else "%.1f Mo", mo)
    val go = mo / 1024.0
    return String.format(Locale.getDefault(), if (en) "%.2f GB" else "%.2f Go", go)
}

