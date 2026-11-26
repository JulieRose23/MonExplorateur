package com.monexplorateur.app.notifications

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.monexplorateur.app.data.ElementFichier
import com.monexplorateur.app.data.UnifiedFileEngine
import com.monexplorateur.app.utils.FileUtils
import com.monexplorateur.app.utils.FormatArchive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class EtatOperationArchive(
    val enCours: Boolean = false,
    val estCompression: Boolean = true,
    val nomArchive: String = "",
    val progression: Int = 0,
    val indetermine: Boolean = false,
    val termine: Boolean = false,
    val succes: Boolean = false,
    val messageFin: String = ""
)

object ArchiveTaskManager {
    private val _etat = MutableStateFlow(EtatOperationArchive())
    val etat: StateFlow<EtatOperationArchive> = _etat.asStateFlow()

    private val _evenementRafraichir = MutableStateFlow(0)
    val evenementRafraichir: StateFlow<Int> = _evenementRafraichir.asStateFlow()

    internal var pendingCompression: PendingCompression? = null
    internal var pendingExtraction: PendingExtraction? = null

    internal data class PendingCompression(
        val cibles: List<ElementFichier>,
        val dossierDestination: String,
        val nomBase: String,
        val format: FormatArchive,
        val motDePasse: String?,
        val tailleSplitMo: Long?,
        val en: Boolean
    )

    internal data class PendingExtraction(
        val archiveItem: ElementFichier,
        val dossierDestination: String,
        val motDePasse: String?,
        val en: Boolean
    )

    fun lancerCompression(
        context: Context,
        cibles: List<ElementFichier>,
        dossierDestination: String,
        nomBase: String,
        format: FormatArchive,
        motDePasse: String?,
        tailleSplitMo: Long?,
        en: Boolean
    ) {
        pendingCompression = PendingCompression(
            cibles = cibles,
            dossierDestination = dossierDestination,
            nomBase = nomBase,
            format = format,
            motDePasse = motDePasse?.takeIf { it.isNotEmpty() },
            tailleSplitMo = tailleSplitMo?.takeIf { it > 0L },
            en = en
        )
        pendingExtraction = null
        val intent = Intent(context, ArchiveForegroundService::class.java).apply {
            action = ArchiveForegroundService.ACTION_COMPRESS
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun lancerExtraction(
        context: Context,
        archiveItem: ElementFichier,
        dossierDestination: String,
        motDePasse: String?,
        en: Boolean
    ) {
        pendingExtraction = PendingExtraction(
            archiveItem = archiveItem,
            dossierDestination = dossierDestination,
            motDePasse = motDePasse?.takeIf { it.isNotEmpty() },
            en = en
        )
        pendingCompression = null
        val intent = Intent(context, ArchiveForegroundService::class.java).apply {
            action = ArchiveForegroundService.ACTION_EXTRACT
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun effacerBanniere() {
        if (!_etat.value.enCours) {
            _etat.value = EtatOperationArchive()
        }
    }

    internal fun majEtat(nouvelEtat: EtatOperationArchive) {
        _etat.value = nouvelEtat
    }

    internal fun notifierFinOperation() {
        _evenementRafraichir.value += 1
    }
}
class ArchiveForegroundService : Service() {

    companion object {
        const val ACTION_COMPRESS = "com.monexplorateur.app.action.COMPRESS"
        const val ACTION_EXTRACT = "com.monexplorateur.app.action.EXTRACT"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var notifHelper: ArchiveNotificationHelper

    override fun onCreate() {
        super.onCreate()
        notifHelper = ArchiveNotificationHelper(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_COMPRESS -> {
                val job = ArchiveTaskManager.pendingCompression ?: run {
                    stopSelf(startId)
                    return START_NOT_STICKY
                }
                val nomComplet = "${job.nomBase}.${job.format.extension}"
                val titre = if (job.en) "Compressing: $nomComplet" else "Compression : $nomComplet"
                val texteInit = if (job.en) "Preparing files..." else "Préparation des fichiers..."
                demarrerForeground(titre, texteInit, 0, true)

                ArchiveTaskManager.majEtat(
                    EtatOperationArchive(
                        enCours = true,
                        estCompression = true,
                        nomArchive = nomComplet,
                        progression = 0,
                        indetermine = true
                    )
                )

                serviceScope.launch {
                    try {
                        val fichiersLocaux = job.cibles.mapNotNull {
                            UnifiedFileEngine.preparerFichierLocal(applicationContext, it)
                        }
                        val dest = File(job.dossierDestination, nomComplet)
                        val ok = FileUtils.compresser(
                            sources = fichiersLocaux,
                            destination = dest,
                            format = job.format,
                            motDePasse = job.motDePasse,
                            tailleSplitMo = job.tailleSplitMo
                        ) { pct ->
                            ArchiveTaskManager.majEtat(
                                EtatOperationArchive(
                                    enCours = true,
                                    estCompression = true,
                                    nomArchive = nomComplet,
                                    progression = pct,
                                    indetermine = false
                                )
                            )
                            notifHelper.showProgress(titre, "$pct %", pct, false)
                        }

                        val titreFin = if (ok) {
                            if (job.en) "Archive created: $nomComplet" else "Archive créée : $nomComplet"
                        } else {
                            if (job.en) "Compression failed: $nomComplet" else "Échec de la compression : $nomComplet"
                        }
                        val msgFin = if (ok) {
                            if (job.en) "Compression completed" else "Compression terminée avec succès"
                        } else {
                            if (job.en) "An error occurred" else "Une erreur est survenue"
                        }

                        ArchiveTaskManager.majEtat(
                            EtatOperationArchive(
                                enCours = false,
                                estCompression = true,
                                nomArchive = nomComplet,
                                progression = if (ok) 100 else 0,
                                termine = true,
                                succes = ok,
                                messageFin = msgFin
                            )
                        )
                        notifHelper.showCompleted(titreFin, msgFin, ok)
                        if (ok) ArchiveTaskManager.notifierFinOperation()
                    } finally {
                        arreterService(startId)
                    }
                }
            }

            ACTION_EXTRACT -> {
                val job = ArchiveTaskManager.pendingExtraction ?: run {
                    stopSelf(startId)
                    return START_NOT_STICKY
                }
                val nom = job.archiveItem.nom
                val titre = if (job.en) "Extracting: $nom" else "Extraction : $nom"
                val texteInit = if (job.en) "Preparing archive..." else "Préparation de l'archive..."
                demarrerForeground(titre, texteInit, 0, true)

                ArchiveTaskManager.majEtat(
                    EtatOperationArchive(
                        enCours = true,
                        estCompression = false,
                        nomArchive = nom,
                        progression = 0,
                        indetermine = true
                    )
                )

                serviceScope.launch {
                    try {
                        val localArchive = UnifiedFileEngine.preparerFichierLocal(applicationContext, job.archiveItem)
                        if (localArchive == null) {
                            val err = if (job.en) "Unable to read archive" else "Impossible de lire l'archive"
                            ArchiveTaskManager.majEtat(
                                EtatOperationArchive(
                                    enCours = false,
                                    estCompression = false,
                                    nomArchive = nom,
                                    termine = true,
                                    succes = false,
                                    messageFin = err
                                )
                            )
                            notifHelper.showCompleted(
                                if (job.en) "Extraction failed: $nom" else "Échec de l'extraction : $nom",
                                err,
                                false
                            )
                            return@launch
                        }

                        val nomDossier = FileUtils.nomSansExtensionArchive(nom)
                        val dossierSortie = File(job.dossierDestination, nomDossier)
                        val ok = FileUtils.decompresser(
                            archive = localArchive,
                            dossierCible = dossierSortie,
                            motDePasse = job.motDePasse
                        ) { pct ->
                            ArchiveTaskManager.majEtat(
                                EtatOperationArchive(
                                    enCours = true,
                                    estCompression = false,
                                    nomArchive = nom,
                                    progression = pct,
                                    indetermine = false
                                )
                            )
                            notifHelper.showProgress(titre, "$pct %", pct, false)
                        }

                        val titreFin = if (ok) {
                            if (job.en) "Extraction completed: $nom" else "Extraction terminée : $nom"
                        } else {
                            if (job.en) "Extraction failed: $nom" else "Échec de l'extraction : $nom"
                        }
                        val msgFin = if (ok) {
                            if (job.en) "Extracted to $nomDossier/" else "Extrait dans $nomDossier/"
                        } else {
                            if (job.en) "Wrong password or corrupted archive?" else "Mot de passe incorrect ou archive corrompue ?"
                        }

                        ArchiveTaskManager.majEtat(
                            EtatOperationArchive(
                                enCours = false,
                                estCompression = false,
                                nomArchive = nom,
                                progression = if (ok) 100 else 0,
                                termine = true,
                                succes = ok,
                                messageFin = msgFin
                            )
                        )
                        notifHelper.showCompleted(titreFin, msgFin, ok)
                        if (ok) ArchiveTaskManager.notifierFinOperation()
                    } finally {
                        arreterService(startId)
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun demarrerForeground(title: String, text: String, progress: Int, indeterminate: Boolean) {
        val notification = notifHelper.buildProgressNotification(title, text, progress, indeterminate)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                ArchiveNotificationHelper.FOREGROUND_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(ArchiveNotificationHelper.FOREGROUND_NOTIFICATION_ID, notification)
        }
    }

    private fun arreterService(startId: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf(startId)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}