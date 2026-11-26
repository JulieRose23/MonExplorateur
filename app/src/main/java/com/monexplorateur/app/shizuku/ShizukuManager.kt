package com.monexplorateur.app.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.monexplorateur.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

data class FichierShizukuInfo(
    val nom: String,
    val cheminComplet: String,
    val estDossier: Boolean,
    val tailleOuNombre: Long,
    val derniereModif: Long
)

object ShizukuManager {
    private const val REQUEST_CODE_SHIZUKU = 2026

    private val _estDisponible = MutableStateFlow(false)
    val estDisponible: StateFlow<Boolean> = _estDisponible

    private val _aPermission = MutableStateFlow(false)
    val aPermission: StateFlow<Boolean> = _aPermission

    private val _serviceConnecte = MutableStateFlow(false)
    val serviceConnecte: StateFlow<Boolean> = _serviceConnecte

    private var fileService: IShizukuFileService? = null
    private var appContext: Context? = null
    private var demanderAuto = false

    private val userServiceConnection = object : ServiceConnection {
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
        verifierEtat(demanderPermission = demanderAuto)
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _estDisponible.value = false
        _aPermission.value = false
        _serviceConnecte.value = false
        fileService = null
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == REQUEST_CODE_SHIZUKU) {
            val accorde = grantResult == PackageManager.PERMISSION_GRANTED
            _aPermission.value = accorde
            if (accorde) lierServiceUtilisateur()
        }
    }

    fun init(context: Context, demanderSiActif: Boolean = false) {
        appContext = context.applicationContext
        demanderAuto = demanderSiActif
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
    }

    fun verifierEtat(demanderPermission: Boolean = false) {
        val pingOk = try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }
        _estDisponible.value = pingOk
        if (!pingOk) {
            _aPermission.value = false
            return
        }

        val granted = try {
            if (Shizuku.isPreV11()) false
            else Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }

        _aPermission.value = granted
        if (granted) {
            lierServiceUtilisateur()
        } else if (demanderPermission) {
            try {
                Shizuku.requestPermission(REQUEST_CODE_SHIZUKU)
            } catch (_: Throwable) {
            }
        }
    }

    fun demanderPermissionDepuisReglages(): String {
        demanderAuto = true
        val pingOk = try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }
        _estDisponible.value = pingOk
        if (!pingOk) {
            return "Service Shizuku inactif : ouvrez l'application Shizuku et démarrez le service."
        }
        val granted = try {
            !Shizuku.isPreV11() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
        if (granted) {
            _aPermission.value = true
            lierServiceUtilisateur()
            return "Shizuku connecté !"
        }
        return try {
            Shizuku.requestPermission(REQUEST_CODE_SHIZUKU)
            "Demande d'autorisation envoyée à Shizuku..."
        } catch (e: Throwable) {
            "Erreur Shizuku : ${e.message}"
        }
    }

    fun deconnecter() {
        demanderAuto = false
        try {
            val ctx = appContext ?: return
            val args = Shizuku.UserServiceArgs(ComponentName(ctx, ShizukuFileService::class.java))
                .daemon(false)
                .processNameSuffix("file_service")
                .debuggable(BuildConfig.DEBUG)
                .version(BuildConfig.VERSION_CODE)
            Shizuku.unbindUserService(args, userServiceConnection, true)
        } catch (_: Throwable) {
        }
        fileService = null
        _serviceConnecte.value = false
    }

    private fun lierServiceUtilisateur() {
        if (_serviceConnecte.value && fileService != null) return
        val ctx = appContext ?: return
        try {
            val args = Shizuku.UserServiceArgs(ComponentName(ctx, ShizukuFileService::class.java))
                .daemon(false)
                .processNameSuffix("file_service")
                .debuggable(BuildConfig.DEBUG)
                .version(BuildConfig.VERSION_CODE)
            Shizuku.bindUserService(args, userServiceConnection)
        } catch (_: Throwable) {
        }
    }

    suspend fun listerFichiers(chemin: String): List<FichierShizukuInfo> = withContext(Dispatchers.IO) {
        val srv = fileService ?: return@withContext emptyList()
        val lignes = try {
            srv.listerFichiers(chemin)
        } catch (_: Throwable) {
            emptyList()
        }
        lignes.mapNotNull { ligne ->
            val parts = ligne.split("|")
            if (parts.size < 4) return@mapNotNull null
            val nom = parts[0]
            val estDir = parts[1] == "1"
            val taille = parts[2].toLongOrNull() ?: 0L
            val modif = parts[3].toLongOrNull() ?: 0L
            val complet = if (chemin.endsWith("/")) "$chemin$nom" else "$chemin/$nom"
            FichierShizukuInfo(nom, complet, estDir, taille, modif)
        }
    }

    suspend fun creerDossier(chemin: String): Boolean = withContext(Dispatchers.IO) {
        try { fileService?.creerDossier(chemin) ?: false } catch (_: Throwable) { false }
    }

    suspend fun supprimer(chemin: String): Boolean = withContext(Dispatchers.IO) {
        try { fileService?.supprimer(chemin) ?: false } catch (_: Throwable) { false }
    }

    suspend fun renommer(ancienChemin: String, nouveauChemin: String): Boolean = withContext(Dispatchers.IO) {
        try { fileService?.renommer(ancienChemin, nouveauChemin) ?: false } catch (_: Throwable) { false }
    }

    suspend fun copier(source: String, destination: String): Boolean = withContext(Dispatchers.IO) {
        try { fileService?.copier(source, destination) ?: false } catch (_: Throwable) { false }
    }

    suspend fun copierVersCache(source: String, destinationCache: String): Boolean = withContext(Dispatchers.IO) {
        try { fileService?.copierVersCache(source, destinationCache) ?: false } catch (_: Throwable) { false }
    }
}
