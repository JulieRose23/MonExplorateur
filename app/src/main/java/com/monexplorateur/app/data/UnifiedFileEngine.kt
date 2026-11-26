package com.monexplorateur.app.data

import android.content.Context
import android.os.Environment
import com.monexplorateur.app.root.RootManager
import com.monexplorateur.app.shizuku.ShizukuManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

enum class SourceFichier(val badge: String) {
    STANDARD("Standard"),
    SHIZUKU("Shizuku"),
    ROOT("Root")
}

data class ElementFichier(
    val nom: String,
    val cheminComplet: String,
    val estDossier: Boolean,
    val tailleOuNombre: Long,
    val derniereModif: Long,
    val source: SourceFichier
)

object UnifiedFileEngine {

    val racineStockage: String
        get() = Environment.getExternalStorageDirectory().absolutePath

    private fun estCheminProtegeAndroid(chemin: String): Boolean {
        val base = racineStockage
        return chemin == "$base/Android/data" ||
            chemin.startsWith("$base/Android/data/") ||
            chemin == "$base/Android/obb" ||
            chemin.startsWith("$base/Android/obb/")
    }

    private fun estHorsStockageInterne(chemin: String): Boolean {
        val base = racineStockage
        return chemin != base && !chemin.startsWith("$base/")
    }

    fun determinerSource(chemin: String): SourceFichier {
        if (AppPreferences.rootActive.value && RootManager.serviceConnecte.value) {
            if (estHorsStockageInterne(chemin) || estCheminProtegeAndroid(chemin)) {
                return SourceFichier.ROOT
            }
        }
        if (AppPreferences.shizukuActive.value && ShizukuManager.serviceConnecte.value) {
            if (estCheminProtegeAndroid(chemin)) {
                return SourceFichier.SHIZUKU
            }
        }
        return SourceFichier.STANDARD
    }

    suspend fun listerDossier(chemin: String): List<ElementFichier> = withContext(Dispatchers.IO) {
        val source = determinerSource(chemin)
        when (source) {
            SourceFichier.ROOT -> {
                RootManager.listerFichiers(chemin).map {
                    ElementFichier(it.nom, it.cheminComplet, it.estDossier, it.tailleOuNombre, it.derniereModif, SourceFichier.ROOT)
                }
            }
            SourceFichier.SHIZUKU -> {
                ShizukuManager.listerFichiers(chemin).map {
                    ElementFichier(it.nom, it.cheminComplet, it.estDossier, it.tailleOuNombre, it.derniereModif, SourceFichier.SHIZUKU)
                }
            }
            SourceFichier.STANDARD -> {
                val dir = File(chemin)
                val enfants = dir.listFiles() ?: return@withContext emptyList()
                enfants.map { f ->
                    val countOrSize = if (f.isDirectory) (f.list()?.size?.toLong() ?: 0L) else f.length()
                    ElementFichier(f.name, f.absolutePath, f.isDirectory, countOrSize, f.lastModified(), SourceFichier.STANDARD)
                }.sortedWith(compareBy({ !it.estDossier }, { it.nom.lowercase(Locale.getDefault()) }))
            }
        }
    }

    suspend fun rechercherPartout(requete: String, limite: Int = 150): List<ElementFichier> = withContext(Dispatchers.IO) {
        val terme = requete.trim().lowercase(Locale.getDefault())
        if (terme.isEmpty()) return@withContext emptyList()

        val resultats = mutableListOf<ElementFichier>()
        val pile = ArrayDeque<String>()
        val depart = if (AppPreferences.rootActive.value && RootManager.serviceConnecte.value) "/" else racineStockage
        pile.add(depart)

        val dossiersSystemeIgnores = setOf("/proc", "/sys", "/dev", "/acct", "/mnt", "/apex")

        while (pile.isNotEmpty() && resultats.size < limite) {
            val courant = pile.removeFirst()
            if (courant in dossiersSystemeIgnores) continue

            val enfants = listerDossier(courant)
            for (enfant in enfants) {
                if (enfant.nom.lowercase(Locale.getDefault()).contains(terme)) {
                    resultats.add(enfant)
                    if (resultats.size >= limite) break
                }
                if (enfant.estDossier && !enfant.nom.startsWith(".")) {
                    pile.addLast(enfant.cheminComplet)
                }
            }
        }
        resultats
    }

    suspend fun creerDossier(chemin: String): Boolean = withContext(Dispatchers.IO) {
        when (determinerSource(chemin)) {
            SourceFichier.ROOT -> RootManager.creerDossier(chemin)
            SourceFichier.SHIZUKU -> ShizukuManager.creerDossier(chemin)
            SourceFichier.STANDARD -> File(chemin).mkdirs()
        }
    }

    suspend fun supprimer(element: ElementFichier): Boolean = withContext(Dispatchers.IO) {
        when (element.source) {
            SourceFichier.ROOT -> RootManager.supprimer(element.cheminComplet)
            SourceFichier.SHIZUKU -> ShizukuManager.supprimer(element.cheminComplet)
            SourceFichier.STANDARD -> {
                val f = File(element.cheminComplet)
                if (f.isDirectory) f.deleteRecursively() else f.delete()
            }
        }
    }

    suspend fun renommer(element: ElementFichier, nouveauNom: String): Boolean = withContext(Dispatchers.IO) {
        val parent = File(element.cheminComplet).parent ?: "/"
        val dest = if (parent == "/") "/$nouveauNom" else "$parent/$nouveauNom"
        when (element.source) {
            SourceFichier.ROOT -> RootManager.renommer(element.cheminComplet, dest)
            SourceFichier.SHIZUKU -> ShizukuManager.renommer(element.cheminComplet, dest)
            SourceFichier.STANDARD -> File(element.cheminComplet).renameTo(File(dest))
        }
    }

    suspend fun copierOuDeplacer(
        elements: List<ElementFichier>,
        dossierDestination: String,
        couper: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        val sourceDest = determinerSource(dossierDestination)
        var toutOk = true

        for (el in elements) {
            val dest = if (dossierDestination == "/") "/${el.nom}" else "$dossierDestination/${el.nom}"
            if (el.cheminComplet == dest) continue

            val ok = if (el.source == SourceFichier.STANDARD && sourceDest == SourceFichier.STANDARD) {
                val srcFile = File(el.cheminComplet)
                val dstFile = File(dest)
                if (dstFile.exists()) false
                else if (couper) {
                    srcFile.renameTo(dstFile) || (srcFile.copyRecursively(dstFile, false) && srcFile.deleteRecursively())
                } else {
                    srcFile.copyRecursively(dstFile, false)
                }
            } else if (el.source == SourceFichier.ROOT || sourceDest == SourceFichier.ROOT) {
                if (couper) {
                    RootManager.renommer(el.cheminComplet, dest) ||
                        (RootManager.copier(el.cheminComplet, dest) && RootManager.supprimer(el.cheminComplet))
                } else {
                    RootManager.copier(el.cheminComplet, dest)
                }
            } else {
                if (couper) {
                    ShizukuManager.renommer(el.cheminComplet, dest) ||
                        (ShizukuManager.copier(el.cheminComplet, dest) && ShizukuManager.supprimer(el.cheminComplet))
                } else {
                    ShizukuManager.copier(el.cheminComplet, dest)
                }
            }
            if (!ok) toutOk = false
        }
        toutOk
    }

    suspend fun preparerFichierLocal(context: Context, element: ElementFichier): File? = withContext(Dispatchers.IO) {
        when (element.source) {
            SourceFichier.STANDARD -> File(element.cheminComplet)
            SourceFichier.SHIZUKU -> {
                val cache = File(context.cacheDir, "preview_shizuku/${element.nom}")
                if (ShizukuManager.copierVersCache(element.cheminComplet, cache.absolutePath)) cache else null
            }
            SourceFichier.ROOT -> {
                val cache = File(context.cacheDir, "preview_root/${element.nom}")
                if (RootManager.copierVersCache(element.cheminComplet, cache.absolutePath)) cache else null
            }
        }
    }
}
