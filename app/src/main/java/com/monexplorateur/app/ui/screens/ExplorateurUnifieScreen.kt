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
import com.monexplorateur.app.data.SourceFichier
import com.monexplorateur.app.data.TrashManager
import com.monexplorateur.app.data.UnifiedFileEngine
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
            Text("Accès au stockage requis", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Autorise l'accès complet aux fichiers pour explorer le téléphone.",
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
            }) { Text("Accorder la permission") }
        }
        return
    }

    val racineStockage = remember { UnifiedFileEngine.racineStockage }
    var cheminCourant by remember { mutableStateOf(racineStockage) }
    var fichiers by remember { mutableStateOf<List<ElementFichier>>(emptyList()) }
    var chargement by remember { mutableStateOf(true) }
    var compteurRefresh by remember { mutableIntStateOf(0) }

    // Multi-sélection
    var selection by remember { mutableStateOf<Set<String>>(emptySet()) }
    val enModeSelection = selection.isNotEmpty()

    // Recherche globale
    var rechercheActive by remember { mutableStateOf(false) }
    var requeteRecherche by remember { mutableStateOf("") }
    var resultatsRecherche by remember { mutableStateOf<List<ElementFichier>>(emptyList()) }
    var rechercheEnCours by remember { mutableStateOf(false) }

    // Presse-papier & Dialogues
    var pressePapier by remember { mutableStateOf<PressePapierMulti?>(null) }
    var dialogueNouveauDossier by remember { mutableStateOf(false) }
    var elementARenommer by remember { mutableStateOf<ElementFichier?>(null) }
    var elementsASupprimer by remember { mutableStateOf<List<ElementFichier>>(emptyList()) }
    var elementProprietes by remember { mutableStateOf<ElementFichier?>(null) }

    // Archives (Compression & Décompression multi-formats)
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
                    Icon(Icons.Default.Add, contentDescription = "Nouveau dossier")
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
                                text = if (pp.couper) "Déplacer ${pp.elements.size} élément(s) :" else "Copier ${pp.elements.size} élément(s) :",
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
                                Toast.makeText(context, "Traitement en cours...", Toast.LENGTH_SHORT).show()
                                val ok = UnifiedFileEngine.copierOuDeplacer(pp.elements, cheminCourant, pp.couper)
                                Toast.makeText(
                                    context,
                                    if (ok) "Opération terminée" else "Certains éléments n'ont pas pu être traités",
                                    Toast.LENGTH_SHORT
                                ).show()
                                if (pp.couper) pressePapier = null
                                compteurRefresh++
                            }
                        }) { Text("Coller ici") }
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
                                Icon(Icons.Default.Close, contentDescription = "Quitter")
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
                            }) { Text("Tout") }
                            TextButton(onClick = {
                                elementsACompresser = listeAffichee.filter { it.cheminComplet in selection }
                            }) { Text("Archiver") }
                            TextButton(onClick = {
                                val choisis = listeAffichee.filter { it.cheminComplet in selection }
                                pressePapier = PressePapierMulti(choisis, couper = true)
                                selection = emptySet()
                            }) { Text("Déplacer") }
                            IconButton(onClick = {
                                elementsASupprimer = listeAffichee.filter { it.cheminComplet in selection }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
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
                                placeholder = { Text("Rechercher partout...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                rechercheActive = false
                                requeteRecherche = ""
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Fermer")
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
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
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
                                    Icon(Icons.Default.Search, contentDescription = "Rechercher partout")
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
                                    label = { Text("Mémoire interne") }
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
                                        label = { Text("Racine / (Root)") }
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
                                Text("Recherche globale en cours...")
                            }
                        }
                    }
                }
                listeAffichee.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (rechercheActive) "Aucun fichier trouvé" else "Ce dossier est vide",
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
                                        scope.launch { ouvrirElement(context, item) }
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
            title = { Text("Créer une archive (${cibles.size} élément(s))") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nomArchive,
                        onValueChange = { nomArchive = it },
                        label = { Text("Nom de l'archive") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Format de compression :", style = MaterialTheme.typography.labelLarge)
                    FormatArchive.entries.forEach { fmt ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { formatChoisi = fmt }
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(selected = formatChoisi == fmt, onClick = { formatChoisi = fmt })
                            Text(fmt.libelle, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val propre = nomArchive.trim().ifEmpty { "Archive" }
                    elementsACompresser = emptyList()
                    selection = emptySet()
                    scope.launch {
                        Toast.makeText(context, "Compression ${formatChoisi.extension.uppercase()} en cours...", Toast.LENGTH_SHORT).show()
                        val fichiersLocaux = cibles.mapNotNull { UnifiedFileEngine.preparerFichierLocal(context, it) }
                        val dest = File(cheminCourant, "$propre.${formatChoisi.extension}")
                        val ok = withContext(Dispatchers.IO) {
                            FileUtils.compresser(fichiersLocaux, dest, formatChoisi)
                        }
                        if (ok) {
                            compteurRefresh++
                            Toast.makeText(context, "Archive créée : ${dest.name}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Échec de la compression", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) { Text("Compresser") }
            },
            dismissButton = {
                TextButton(onClick = { elementsACompresser = emptyList() }) { Text("Annuler") }
            }
        )
    }

    // Boîte de choix au clic sur une archive (ZIP, 7Z, RAR, TAR.GZ...)
    archiveAExtraire?.let { archiveItem ->
        AlertDialog(
            onDismissRequest = { archiveAExtraire = null },
            title = { Text("Archive : ${archiveItem.nom}") },
            text = { Text("Comment souhaitez-vous traiter cette archive ?") },
            confirmButton = {
                TextButton(onClick = {
                    archiveAExtraire = null
                    archiveDestinationPicker = archiveItem
                }) { Text("Choisir où extraire...") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        archiveAExtraire = null
                        scope.launch { ouvrirElement(context, archiveItem) }
                    }) { Text("Ouvrir") }
                    TextButton(onClick = {
                        val cible = archiveItem
                        archiveAExtraire = null
                        scope.launch {
                            extraireArchiveVers(context, cible, cheminCourant) { compteurRefresh++ }
                        }
                    }) { Text("Extraire ici") }
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
            title = { Text("Extraire vers...") },
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
                        extraireArchiveVers(context, cible, dest) { compteurRefresh++ }
                    }
                }) { Text("Extraire ici") }
            },
            dismissButton = {
                TextButton(onClick = { archiveDestinationPicker = null }) { Text("Annuler") }
            }
        )
    }

    // Dialogue Propriétés + MD5 / SHA-256
    elementProprietes?.let { cible ->
        var md5 by remember { mutableStateOf("Calcul...") }
        var sha256 by remember { mutableStateOf("Calcul...") }

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

        AlertDialog(
            onDismissRequest = { elementProprietes = null },
            title = { Text("Propriétés (${cible.source.badge})") },
            text = {
                SelectionContainer {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Nom : ${cible.nom}", fontWeight = FontWeight.Bold)
                        Text("Chemin : ${cible.cheminComplet}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = if (cible.estDossier) "Type : Dossier"
                            else "Taille : ${formaterTailleUnifie(cible.tailleOuNombre)} (${cible.tailleOuNombre} octets)",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text("Modifié le : $dateTxt", style = MaterialTheme.typography.bodySmall)
                        if (!cible.estDossier) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text("MD5 :", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelSmall)
                            Text(md5, style = MaterialTheme.typography.bodySmall)
                            Text("SHA-256 :", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelSmall)
                            Text(sha256, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { elementProprietes = null }) { Text("Fermer") }
            }
        )
    }

    // Dialogue Nouveau dossier
    if (dialogueNouveauDossier) {
        var nomDossier by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { dialogueNouveauDossier = false },
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
                TextButton(onClick = {
                    val propre = nomDossier.trim()
                    if (propre.isNotEmpty()) {
                        scope.launch {
                            val cible = if (cheminCourant == "/") "/$propre" else "$cheminCourant/$propre"
                            if (UnifiedFileEngine.creerDossier(cible)) compteurRefresh++
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

    // Dialogue Renommer
    elementARenommer?.let { cible ->
        var nouveauNom by remember { mutableStateOf(cible.nom) }
        AlertDialog(
            onDismissRequest = { elementARenommer = null },
            title = { Text("Renommer") },
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
                            else Toast.makeText(context, "Échec du renommage", Toast.LENGTH_SHORT).show()
                        }
                    }
                    elementARenommer = null
                }) { Text("Valider") }
            },
            dismissButton = {
                TextButton(onClick = { elementARenommer = null }) { Text("Annuler") }
            }
        )
    }

    // Dialogue Suppression : déplacement vers la corbeille (ou suppression définitive)
    if (elementsASupprimer.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { elementsASupprimer = emptyList() },
            title = { Text("Mettre à la corbeille (${elementsASupprimer.size}) ?") },
            text = {
                Text(
                    if (elementsASupprimer.size == 1) "Déplacer « ${elementsASupprimer.first().nom} » vers la corbeille ?"
                    else "Déplacer ces ${elementsASupprimer.size} éléments vers la corbeille ?"
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
                        Toast.makeText(context, "$okCount élément(s) déplacé(s) dans la corbeille", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Corbeille", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { elementsASupprimer = emptyList() }) { Text("Annuler") }
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
                            Toast.makeText(context, "$okCount élément(s) supprimé(s) définitivement", Toast.LENGTH_SHORT).show()
                        }
                    }) { Text("Définitif", color = MaterialTheme.colorScheme.error) }
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LigneElementUnifie(
    item: ElementFichier,
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
        item.estDossier -> if (item.tailleOuNombre >= 0) "${item.tailleOuNombre} élément(s) • $dateFormatee" else "Dossier • $dateFormatee"
        else -> "${formaterTailleUnifie(item.tailleOuNombre)} • $dateFormatee"
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
                DropdownMenuItem(text = { Text("Sélectionner") }, onClick = { menuOuvert = false; onLongClick() })
                DropdownMenuItem(text = { Text("Copier") }, onClick = { menuOuvert = false; onCopier() })
                DropdownMenuItem(text = { Text("Déplacer") }, onClick = { menuOuvert = false; onCouper() })
                DropdownMenuItem(text = { Text("Renommer") }, onClick = { menuOuvert = false; onRenommer() })
                if (!item.estDossier && FileUtils.estArchiveSupportee(item.nom)) {
                    DropdownMenuItem(text = { Text("Extraire...") }, onClick = { menuOuvert = false; onExtraire() })
                } else {
                    DropdownMenuItem(text = { Text("Compresser...") }, onClick = { menuOuvert = false; onCompresser() })
                }
                DropdownMenuItem(text = { Text("Propriétés / MD5") }, onClick = { menuOuvert = false; onProprietes() })
                DropdownMenuItem(
                    text = { Text("Supprimer", color = MaterialTheme.colorScheme.error) },
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
    onSucces: () -> Unit
) {
    Toast.makeText(context, "Extraction en cours...", Toast.LENGTH_SHORT).show()
    val localArchive = UnifiedFileEngine.preparerFichierLocal(context, archiveItem)
    if (localArchive == null) {
        Toast.makeText(context, "Impossible de lire l'archive", Toast.LENGTH_SHORT).show()
        return
    }
    val nomDossier = FileUtils.nomSansExtensionArchive(archiveItem.nom)
    val dossierSortie = File(dossierParentDestination, nomDossier)
    val ok = withContext(Dispatchers.IO) {
        FileUtils.decompresser(localArchive, dossierSortie)
    }
    if (ok) {
        onSucces()
        Toast.makeText(context, "Extrait dans $nomDossier/", Toast.LENGTH_SHORT).show()
    } else {
        Toast.makeText(context, "Échec de l'extraction (format chiffré ou corrompu ?)", Toast.LENGTH_LONG).show()
    }
}

private suspend fun ouvrirElement(context: Context, item: ElementFichier) {
    val fichierLocal = UnifiedFileEngine.preparerFichierLocal(context, item)
    if (fichierLocal == null || !fichierLocal.exists()) {
        Toast.makeText(context, "Impossible de préparer ce fichier", Toast.LENGTH_SHORT).show()
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
        context.startActivity(Intent.createChooser(intent, "Ouvrir avec"))
    } catch (_: Exception) {
        Toast.makeText(context, "Aucune application compatible", Toast.LENGTH_SHORT).show()
    }
}

private fun formaterTailleUnifie(octets: Long): String {
    if (octets < 1024) return "$octets o"
    val ko = octets / 1024.0
    if (ko < 1024) return String.format(Locale.getDefault(), "%.1f Ko", ko)
    val mo = ko / 1024.0
    if (mo < 1024) return String.format(Locale.getDefault(), "%.1f Mo", mo)
    val go = mo / 1024.0
    return String.format(Locale.getDefault(), "%.2f Go", go)
}

