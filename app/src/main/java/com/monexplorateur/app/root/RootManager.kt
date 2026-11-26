package com.monexplorateur.app.root

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.monexplorateur.app.IShizukuFileService
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

data class FichierRoot(
    val nom: String,
    val cheminComplet: String,
    val estDossier: Boolean,
    val tailleOuNombre: Long,
    val derniereModif: Long
)

object RootManager {
    private val _aAccesRoot = MutableStateFlow<Boolean?>(null)
    val aAccesRoot: StateFlow<Boolean?> = _aAccesRoot

    private val _serviceConnecte = MutableStateFlow(false)
    val serviceConnecte: StateFlow<Boolean> = _serviceConnecte

    private var rootFileService: IShizukuFileService? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (binder != null && binder.pingBinder()) {
                rootFileService = IShizukuFileService.Stub.asInterface(binder)
                _serviceConnecte.value = true
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            rootFileService = null
            _serviceConnecte.value = false
        }
    }

    suspend fun verifierEtConnecter(context: Context) {
        val estRoot = withContext(Dispatchers.IO) {
            Shell.getShell().isRoot
        }
        _aAccesRoot.value = estRoot
        if (estRoot && rootFileService == null) {
            withContext(Dispatchers.Main) {
                val intent = Intent(context, RootFileService::class.java)
                RootService.bind(intent, connection)
            }
        }
    }

    fun listerFichiers(chemin: String): List<FichierRoot> {
        val brut = try {
            rootFileService?.listFiles(chemin) ?: ""
        } catch (e: Exception) {
            ""
        }
        if (brut.isBlank()) return emptyList()

        return brut.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { ligne ->
                val parts = ligne.split("|", limit = 4)
                if (parts.size < 4) return@mapNotNull null
                val estDossier = parts[0] == "1"
                val taille = parts[1].toLongOrNull() ?: 0L
                val modif = parts[2].toLongOrNull() ?: 0L
                val nom = parts[3]
                val cheminEnfant = if (chemin == "/") "/$nom" else "$chemin/$nom"
                FichierRoot(nom, cheminEnfant, estDossier, taille, modif)
            }
            .sortedWith(compareBy({ !it.estDossier }, { it.nom.lowercase() }))
    }

    fun creerDossier(chemin: String): Boolean = try { rootFileService?.createDirectory(chemin) == true } catch (_: Exception) { false }
    fun supprimer(chemin: String): Boolean = try { rootFileService?.deletePath(chemin) == true } catch (_: Exception) { false }
    fun renommer(ancien: String, nouveau: String): Boolean = try { rootFileService?.renamePath(ancien, nouveau) == true } catch (_: Exception) { false }
    fun copier(source: String, dest: String): Boolean = try { rootFileService?.copyPath(source, dest) == true } catch (_: Exception) { false }
    fun copierVersCache(source: String, destCache: String): Boolean = try { rootFileService?.copyToLocalCache(source, destCache) == true } catch (_: Exception) { false }
}
