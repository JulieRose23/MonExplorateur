package com.monexplorateur.app.shizuku

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.monexplorateur.app.BuildConfig
import com.monexplorateur.app.IShizukuFileService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import rikka.shizuku.Shizuku

data class FichierShizuku(
    val nom: String,
    val cheminComplet: String,
    val estDossier: Boolean,
    val tailleOuNombre: Long,
    val derniereModif: Long
)

object ShizukuManager {
    private var estInitialise = false

    private val _estDisponible = MutableStateFlow(false)
    val estDisponible: StateFlow<Boolean> = _estDisponible

    private val _aPermission = MutableStateFlow(false)
    val aPermission: StateFlow<Boolean> = _aPermission

    private val _serviceConnecte = MutableStateFlow(false)
    val serviceConnecte: StateFlow<Boolean> = _serviceConnecte

    private var fileService: IShizukuFileService? = null

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(BuildConfig.APPLICATION_ID, ShizukuFileService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("shizuku_files")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (binder != null && binder.pingBinder()) {
                fileService = IShizukuFileService.Stub.asInterface(binder)
                _serviceConnecte.value = true
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            fileService = null
            _serviceConnecte.value = false
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        verifierEtat()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _estDisponible.value = false
        _aPermission.value = false
        _serviceConnecte.value = false
        fileService = null
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        _aPermission.value = (grantResult == PackageManager.PERMISSION_GRANTED)
        if (_aPermission.value) {
            lierService()
        }
    }

    fun initialiser() {
        if (!estInitialise) {
            estInitialise = true
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        }
        verifierEtat()
    }

    fun verifierEtat() {
        val pingOk = try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
        _estDisponible.value = pingOk
        if (pingOk) {
            val permOk = try {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } catch (e: Exception) {
                false
            }
            _aPermission.value = permOk
            if (permOk) {
                if (fileService == null) lierService()
            } else {
                // Déclenche automatiquement la popup Shizuku dès que le service est détecté
                demanderPermission()
            }
        } else {
            _aPermission.value = false
            _serviceConnecte.value = false
        }
    }

    fun demanderPermission() {
        try {
            if (!Shizuku.pingBinder()) return
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(1001)
            } else {
                _aPermission.value = true
                lierService()
            }
        } catch (_: Exception) {
        }
    }

    fun lierService() {
        try {
            if (!Shizuku.pingBinder()) return
            Shizuku.bindUserService(userServiceArgs, serviceConnection)
        } catch (_: Exception) {
        }
    }

    fun listerFichiers(chemin: String): List<FichierShizuku> {
        val brut = try {
            fileService?.listFiles(chemin) ?: ""
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
                val cheminEnfant = if (chemin.endsWith("/")) "$chemin$nom" else "$chemin/$nom"
                FichierShizuku(nom, cheminEnfant, estDossier, taille, modif)
            }
            .sortedWith(compareBy({ !it.estDossier }, { it.nom.lowercase() }))
    }

    fun creerDossier(chemin: String): Boolean = try { fileService?.createDirectory(chemin) == true } catch (_: Exception) { false }
    fun supprimer(chemin: String): Boolean = try { fileService?.deletePath(chemin) == true } catch (_: Exception) { false }
    fun renommer(ancien: String, nouveau: String): Boolean = try { fileService?.renamePath(ancien, nouveau) == true } catch (_: Exception) { false }
    fun copier(source: String, dest: String): Boolean = try { fileService?.copyPath(source, dest) == true } catch (_: Exception) { false }
    fun copierVersCache(source: String, destCache: String): Boolean = try { fileService?.copyToLocalCache(source, destCache) == true } catch (_: Exception) { false }
}

