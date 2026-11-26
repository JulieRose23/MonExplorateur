package com.monexplorateur.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.FileProvider
import com.monexplorateur.app.data.AppPreferences
import com.monexplorateur.app.data.ElementFichier
import com.monexplorateur.app.data.LangueApp
import com.monexplorateur.app.data.SourceFichier
import com.monexplorateur.app.notifications.ArchiveTaskManager
import com.monexplorateur.app.ui.components.IconeDossierModerne
import com.monexplorateur.app.ui.screens.CorbeilleScreen
import com.monexplorateur.app.ui.screens.ExplorateurUnifieScreen
import com.monexplorateur.app.ui.screens.ReglagesScreen
import com.monexplorateur.app.ui.screens.ReseauFtpScreen
import com.monexplorateur.app.ui.theme.MonExplorateurTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

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
    private var uriRecue by mutableStateOf<Uri?>(null)
    private var modeSelectionExterne by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (intent?.action == Intent.ACTION_VIEW) {
            uriRecue = intent.data
        }
        modeSelectionExterne = intent?.action == Intent.ACTION_GET_CONTENT

        setContent {
            val modeTheme by AppPreferences.modeTheme.collectAsState()
            MonExplorateurTheme(modeTheme = modeTheme) {
                ApplicationPrincipale(
                    uriExterne = uriRecue,
                    onUriTraitee = { uriRecue = null },
                    modeSelectionExterne = modeSelectionExterne,
                    onFichierChoisiPourApp = { fichierLocal ->
                        val uriResultat = FileProvider.getUriForFile(
                            this,
                            "${packageName}.fileprovider",
                            fichierLocal
                        )
                        val resultIntent = Intent().apply {
                            data = uriResultat
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
            uriRecue = intent.data
        }
        if (intent.action == Intent.ACTION_GET_CONTENT) {
            modeSelectionExterne = true
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationPrincipale(
    uriExterne: Uri? = null,
    onUriTraitee: () -> Unit = {},
    modeSelectionExterne: Boolean = false,
    onFichierChoisiPourApp: ((File) -> Unit)? = null
) {
    var ongletActif by remember { mutableStateOf(OngletPrincipal.STOCKAGE) }
    val langue by AppPreferences.langue.collectAsState()
    val en = langue == LangueApp.EN
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(uriExterne) {
        val uri = uriExterne ?: return@LaunchedEffect
        ongletActif = OngletPrincipal.STOCKAGE
        val element = withContext(Dispatchers.IO) {
            resoudreUriArchive(context, uri)
        }
        if (element != null) {
            ArchiveTaskManager.ouvrirArchiveExterne(element)
        }
        onUriTraitee()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            modeSelectionExterne -> if (en) "Select a file" else "Choisir un fichier"
                            ongletActif == OngletPrincipal.STOCKAGE -> "MonExplorateur"
                            ongletActif == OngletPrincipal.CORBEILLE -> if (en) "Trash" else "Corbeille"
                            ongletActif == OngletPrincipal.FTP -> if (en) "FTP Network" else "Réseau FTP"
                            else -> if (en) "Settings" else "Réglages"
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
            if (!modeSelectionExterne) {
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
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            val basPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding())
            when (ongletActif) {
                OngletPrincipal.STOCKAGE -> ExplorateurUnifieScreen(
                    paddingBasNavigation = basPadding,
                    modeSelectionExterne = modeSelectionExterne,
                    onFichierChoisiExterne = onFichierChoisiPourApp
                )
                OngletPrincipal.CORBEILLE -> CorbeilleScreen(paddingBasNavigation = basPadding)
                OngletPrincipal.FTP -> Box(modifier = Modifier.padding(basPadding)) { ReseauFtpScreen() }
                OngletPrincipal.REGLAGES -> ReglagesScreen(paddingBasNavigation = basPadding)
            }
        }
    }
}

private fun resoudreUriArchive(context: android.content.Context, uri: Uri): ElementFichier? {
    return try {
        if (uri.scheme == "file" && uri.path != null) {
            val f = File(uri.path!!)
            if (f.exists()) {
                return ElementFichier(
                    nom = f.name,
                    cheminComplet = f.absolutePath,
                    estDossier = false,
                    tailleOuNombre = f.length(),
                    derniereModif = f.lastModified(),
                    source = SourceFichier.STANDARD
                )
            }
        }

        var nomFichier = "archive.zip"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                nomFichier = cursor.getString(idx) ?: nomFichier
            }
        }

        val dossierDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val fichierDownload = File(dossierDownloads, nomFichier)
        if (fichierDownload.exists() && fichierDownload.isFile) {
            return ElementFichier(
                nom = fichierDownload.name,
                cheminComplet = fichierDownload.absolutePath,
                estDossier = false,
                tailleOuNombre = fichierDownload.length(),
                derniereModif = fichierDownload.lastModified(),
                source = SourceFichier.STANDARD
            )
        }

        val dossierCache = File(context.cacheDir, "archives_externes").apply { mkdirs() }
        val copieLocale = File(dossierCache, nomFichier)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(copieLocale).use { output -> input.copyTo(output, 65536) }
        }
        if (copieLocale.exists()) {
            ElementFichier(
                nom = copieLocale.name,
                cheminComplet = copieLocale.absolutePath,
                estDossier = false,
                tailleOuNombre = copieLocale.length(),
                derniereModif = copieLocale.lastModified(),
                source = SourceFichier.STANDARD
            )
        } else null
    } catch (_: Exception) {
        null
    }
}