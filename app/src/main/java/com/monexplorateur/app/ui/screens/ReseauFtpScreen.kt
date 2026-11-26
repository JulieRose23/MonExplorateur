package com.monexplorateur.app.ui.screens

import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.monexplorateur.app.ftp.ServeurFtpManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

@Composable
fun ReseauFtpScreen() {
    var ongletSelectionne by remember { mutableIntStateOf(0) }
    val onglets = listOf("Serveur FTP (Téléphone)", "Client FTP (Distant)")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = ongletSelectionne) {
            onglets.forEachIndexed { index, titre ->
                Tab(
                    selected = ongletSelectionne == index,
                    onClick = { ongletSelectionne = index },
                    text = { Text(titre) }
                )
            }
        }

        when (ongletSelectionne) {
            0 -> OngletServeurFtp()
            1 -> OngletClientFtp()
        }
    }
}

@Composable
private fun OngletServeurFtp() {
    val context = LocalContext.current
    val enCours by ServeurFtpManager.enCours.collectAsState()
    val adresseUrl by ServeurFtpManager.adresseUrl.collectAsState()

    var portTexte by remember { mutableStateOf("2221") }
    var utilisateur by remember { mutableStateOf("android") }
    var motDePasse by remember { mutableStateOf("1234") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (enCours) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (enCours) "Serveur FTP actif" else "Serveur FTP arrêté",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (enCours) {
                    Text(
                        text = "Adresse : $adresseUrl",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Identifiants : $utilisateur / $motDePasse",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = "Démarre le serveur pour accéder à la mémoire du téléphone depuis un PC sur le même Wi-Fi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        OutlinedTextField(
            value = portTexte,
            onValueChange = { portTexte = it },
            label = { Text("Port (défaut : 2221)") },
            enabled = !enCours,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = utilisateur,
                onValueChange = { utilisateur = it },
                label = { Text("Utilisateur") },
                enabled = !enCours,
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = motDePasse,
                onValueChange = { motDePasse = it },
                label = { Text("Mot de passe") },
                enabled = !enCours,
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (!enCours) {
            Button(
                onClick = {
                    val port = portTexte.toIntOrNull() ?: 2221
                    val res = ServeurFtpManager.demarrer(port, utilisateur.trim(), motDePasse.trim())
                    res.onFailure {
                        Toast.makeText(context, "Erreur : ${it.message}", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Démarrer le serveur FTP")
            }
        } else {
            Button(
                onClick = { ServeurFtpManager.arreter() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Arrêter le serveur FTP")
            }
        }
    }
}

@Composable
private fun OngletClientFtp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hote by remember { mutableStateOf("") }
    var portTexte by remember { mutableStateOf("21") }
    var utilisateur by remember { mutableStateOf("anonymous") }
    var motDePasse by remember { mutableStateOf("") }

    var connecte by remember { mutableStateOf(false) }
    var chargement by remember { mutableStateOf(false) }
    var cheminDistant by remember { mutableStateOf("/") }
    var fichiersDistants by remember { mutableStateOf<List<FTPFile>>(emptyList()) }

    suspend fun chargerDossierDistant(nouveauChemin: String) {
        chargement = true
        val resultat = withContext(Dispatchers.IO) {
            val client = FTPClient()
            try {
                client.connectTimeout = 8000
                client.connect(hote.trim(), portTexte.toIntOrNull() ?: 21)
                if (!client.login(utilisateur.trim(), motDePasse)) {
                    throw Exception("Identifiants incorrects")
                }
                client.enterLocalPassiveMode()
                client.setFileType(FTP.BINARY_FILE_TYPE)
                client.changeWorkingDirectory(nouveauChemin)
                val rep = client.printWorkingDirectory() ?: nouveauChemin
                val liste = client.listFiles()
                    ?.filter { it.name != "." && it.name != ".." }
                    ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                    ?: emptyList()
                Result.success(rep to liste)
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                try {
                    if (client.isConnected) {
                        client.logout()
                        client.disconnect()
                    }
                } catch (_: Exception) {
                }
            }
        }
        chargement = false
        resultat.onSuccess { (rep, liste) ->
            connecte = true
            cheminDistant = rep
            fichiersDistants = liste
        }.onFailure {
            Toast.makeText(context, "Connexion FTP échouée : ${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    if (!connecte) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Connexion à un serveur FTP", style = MaterialTheme.typography.titleMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = hote,
                    onValueChange = { hote = it },
                    label = { Text("Hôte / IP (ex: 192.168.1.10)") },
                    singleLine = true,
                    modifier = Modifier.weight(2f)
                )
                OutlinedTextField(
                    value = portTexte,
                    onValueChange = { portTexte = it },
                    label = { Text("Port") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = utilisateur,
                    onValueChange = { utilisateur = it },
                    label = { Text("Utilisateur") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = motDePasse,
                    onValueChange = { motDePasse = it },
                    label = { Text("Mot de passe") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Button(
                onClick = {
                    if (hote.isNotBlank()) {
                        scope.launch { chargerDossierDistant("/") }
                    }
                },
                enabled = !chargement && hote.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (chargement) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Se connecter")
                }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                if (cheminDistant != "/") {
                    IconButton(onClick = {
                        val parent = File(cheminDistant).parent ?: "/"
                        scope.launch { chargerDossierDistant(parent) }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Parent")
                    }
                }
                Text(
                    text = "${hote.trim()}:$cheminDistant",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(onClick = {
                    connecte = false
                    fichiersDistants = emptyList()
                }) {
                    Text("Déconnecter")
                }
            }
            HorizontalDivider()

            if (chargement) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(fichiersDistants, key = { it.name }) { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (item.isDirectory) {
                                        val cible = if (cheminDistant == "/") "/${item.name}" else "$cheminDistant/${item.name}"
                                        scope.launch { chargerDossierDistant(cible) }
                                    } else {
                                        scope.launch {
                                            telechargerFichierFtp(
                                                context = context,
                                                hote = hote.trim(),
                                                port = portTexte.toIntOrNull() ?: 21,
                                                user = utilisateur.trim(),
                                                pass = motDePasse,
                                                dossierDistant = cheminDistant,
                                                nomFichier = item.name
                                            )
                                        }
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = if (item.isDirectory) Icons.AutoMirrored.Filled.List else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (item.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    text = if (item.isDirectory) "Dossier distant" else "${formaterTailleFtp(item.size)} (Appuyer pour télécharger dans Download)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

private suspend fun telechargerFichierFtp(
    context: android.content.Context,
    hote: String,
    port: Int,
    user: String,
    pass: String,
    dossierDistant: String,
    nomFichier: String
) {
    Toast.makeText(context, "Téléchargement de $nomFichier...", Toast.LENGTH_SHORT).show()
    val ok = withContext(Dispatchers.IO) {
        val client = FTPClient()
        try {
            client.connect(hote, port)
            if (!client.login(user, pass)) return@withContext false
            client.enterLocalPassiveMode()
            client.setFileType(FTP.BINARY_FILE_TYPE)
            client.changeWorkingDirectory(dossierDistant)

            val dossierDownload = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dossierDownload.mkdirs()
            val fichierLocal = File(dossierDownload, nomFichier)
            FileOutputStream(fichierLocal).use { out ->
                client.retrieveFile(nomFichier, out)
            }
        } catch (_: Exception) {
            false
        } finally {
            try {
                if (client.isConnected) {
                    client.logout()
                    client.disconnect()
                }
            } catch (_: Exception) {
            }
        }
    }
    Toast.makeText(
        context,
        if (ok) "Enregistré dans Download/$nomFichier" else "Échec du téléchargement",
        Toast.LENGTH_LONG
    ).show()
}

private fun formaterTailleFtp(octets: Long): String {
    if (octets < 1024) return "$octets o"
    val ko = octets / 1024.0
    if (ko < 1024) return String.format(Locale.getDefault(), "%.1f Ko", ko)
    val mo = ko / 1024.0
    if (mo < 1024) return String.format(Locale.getDefault(), "%.1f Mo", mo)
    val go = mo / 1024.0
    return String.format(Locale.getDefault(), "%.2f Go", go)
}

