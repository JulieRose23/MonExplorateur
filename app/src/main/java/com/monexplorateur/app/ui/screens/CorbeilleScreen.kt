package com.monexplorateur.app.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.monexplorateur.app.data.AppPreferences
import com.monexplorateur.app.data.ElementCorbeille
import com.monexplorateur.app.data.LangueApp
import com.monexplorateur.app.data.TrashManager
import com.monexplorateur.app.ui.components.BadgeIconeElement
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val FORMATEUR_DATE_CORBEILLE = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

@Composable
fun CorbeilleScreen(paddingBasNavigation: PaddingValues = PaddingValues(0.dp)) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val langue by AppPreferences.langue.collectAsState()
    val en = langue == LangueApp.EN

    var itemsCorbeille by remember { mutableStateOf<List<ElementCorbeille>>(emptyList()) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var confirmerVider by remember { mutableStateOf(false) }
    var itemASupprimerDef by remember { mutableStateOf<ElementCorbeille?>(null) }

    LaunchedEffect(refreshTrigger) {
        itemsCorbeille = TrashManager.listerCorbeille(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingBasNavigation)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = if (en) "${itemsCorbeille.size} item(s) in trash" else "${itemsCorbeille.size} élément(s) dans la corbeille",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (itemsCorbeille.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { confirmerVider = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (en) "Empty" else "Vider")
                    }
                }
            }
        }
        HorizontalDivider()

        if (itemsCorbeille.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (en) "Trash is empty" else "La corbeille est vide",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(itemsCorbeille, key = { it.id }) { item ->
                    val dateSuppr = remember(item.dateSuppression) {
                        FORMATEUR_DATE_CORBEILLE.format(Date(item.dateSuppression))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 8.dp, top = 11.dp, bottom = 11.dp)
                    ) {
                        BadgeIconeElement(
                            nom = item.nomOriginal,
                            estDossier = item.estDossier,
                            estSelectionne = false
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.nomOriginal,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (en) "Deleted on $dateSuppr" else "Supprimé le $dateSuppr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (en) "Origin: ${item.cheminOriginal}" else "Origine : ${item.cheminOriginal}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = {
                            scope.launch {
                                val ok = TrashManager.restaurer(context, item)
                                Toast.makeText(
                                    context,
                                    if (ok) (if (en) "Restored: ${item.nomOriginal}" else "Restauré : ${item.nomOriginal}")
                                    else (if (en) "Restore failed" else "Échec de la restauration"),
                                    Toast.LENGTH_SHORT
                                ).show()
                                refreshTrigger++
                            }
                        }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = if (en) "Restore" else "Restaurer",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { itemASupprimerDef = item }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = if (en) "Delete permanently" else "Supprimer définitivement",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
    }

    if (confirmerVider) {
        AlertDialog(
            onDismissRequest = { confirmerVider = false },
            title = { Text(if (en) "Empty trash?" else "Vider la corbeille ?") },
            text = {
                Text(
                    if (en) "All items will be permanently deleted and cannot be restored."
                    else "Tous les éléments seront supprimés définitivement et ne pourront plus être restaurés."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmerVider = false
                    scope.launch {
                        TrashManager.viderCorbeille(context)
                        refreshTrigger++
                        Toast.makeText(context, if (en) "Trash emptied" else "Corbeille vidée", Toast.LENGTH_SHORT).show()
                    }
                }) { Text(if (en) "Empty" else "Vider", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmerVider = false }) { Text(if (en) "Cancel" else "Annuler") }
            }
        )
    }

    itemASupprimerDef?.let { cible ->
        AlertDialog(
            onDismissRequest = { itemASupprimerDef = null },
            title = { Text(if (en) "Delete permanently?" else "Supprimer définitivement ?") },
            text = {
                Text(
                    if (en) "Permanently delete \"${cible.nomOriginal}\"?"
                    else "Supprimer « ${cible.nomOriginal} » de façon irréversible ?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    itemASupprimerDef = null
                    scope.launch {
                        TrashManager.supprimerDefinitivement(context, cible)
                        refreshTrigger++
                        Toast.makeText(context, if (en) "Permanently deleted" else "Supprimé définitivement", Toast.LENGTH_SHORT).show()
                    }
                }) { Text(if (en) "Delete" else "Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { itemASupprimerDef = null }) { Text(if (en) "Cancel" else "Annuler") }
            }
        )
    }
}
