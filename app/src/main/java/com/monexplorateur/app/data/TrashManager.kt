package com.monexplorateur.app.data

import android.content.Context
import com.monexplorateur.app.root.RootManager
import com.monexplorateur.app.shizuku.ShizukuManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class ElementCorbeille(
    val id: String,
    val nomOriginal: String,
    val cheminOriginal: String,
    val cheminStockageCorbeille: String,
    val estDossier: Boolean,
    val taille: Long,
    val dateSuppression: Long
)

object TrashManager {
    private const val PREFS_CORBEILLE = "mon_explorateur_corbeille"
    private const val CLE_ITEMS = "items_json"

    private fun dossierCorbeille(): File {
        val dir = File(UnifiedFileEngine.racineStockage, ".MonExplorateur_Corbeille")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun listerCorbeille(context: Context): List<ElementCorbeille> {
        val prefs = context.getSharedPreferences(PREFS_CORBEILLE, Context.MODE_PRIVATE)
        val raw = prefs.getString(CLE_ITEMS, "[]") ?: "[]"
        val arr = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
        val resultat = mutableListOf<ElementCorbeille>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val cheminStock = obj.optString("cheminStockageCorbeille")
            if (cheminStock.isNotEmpty() && File(cheminStock).exists()) {
                resultat.add(
                    ElementCorbeille(
                        id = obj.optString("id"),
                        nomOriginal = obj.optString("nomOriginal"),
                        cheminOriginal = obj.optString("cheminOriginal"),
                        cheminStockageCorbeille = cheminStock,
                        estDossier = obj.optBoolean("estDossier"),
                        taille = obj.optLong("taille"),
                        dateSuppression = obj.optLong("dateSuppression")
                    )
                )
            }
        }
        return resultat.sortedByDescending { it.dateSuppression }
    }

    private fun sauvegarderListe(context: Context, liste: List<ElementCorbeille>) {
        val arr = JSONArray()
        for (item in liste) {
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("nomOriginal", item.nomOriginal)
                put("cheminOriginal", item.cheminOriginal)
                put("cheminStockageCorbeille", item.cheminStockageCorbeille)
                put("estDossier", item.estDossier)
                put("taille", item.taille)
                put("dateSuppression", item.dateSuppression)
            })
        }
        context.getSharedPreferences(PREFS_CORBEILLE, Context.MODE_PRIVATE)
            .edit()
            .putString(CLE_ITEMS, arr.toString())
            .apply()
    }

    suspend fun mettreALaCorbeille(context: Context, element: ElementFichier): Boolean = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val destCorbeille = File(dossierCorbeille(), "${id}_${element.nom}")

        val deplaceOk = when (element.source) {
            SourceFichier.STANDARD -> {
                val src = File(element.cheminComplet)
                src.renameTo(destCorbeille) || (src.copyRecursively(destCorbeille, true) && src.deleteRecursively())
            }
            SourceFichier.SHIZUKU -> {
                ShizukuManager.renommer(element.cheminComplet, destCorbeille.absolutePath) ||
                    (ShizukuManager.copier(element.cheminComplet, destCorbeille.absolutePath) && ShizukuManager.supprimer(element.cheminComplet))
            }
            SourceFichier.ROOT -> {
                RootManager.renommer(element.cheminComplet, destCorbeille.absolutePath) ||
                    (RootManager.copier(element.cheminComplet, destCorbeille.absolutePath) && RootManager.supprimer(element.cheminComplet))
            }
        }

        if (!deplaceOk) return@withContext false

        val actuelle = listerCorbeille(context).toMutableList()
        actuelle.add(
            ElementCorbeille(
                id = id,
                nomOriginal = element.nom,
                cheminOriginal = element.cheminComplet,
                cheminStockageCorbeille = destCorbeille.absolutePath,
                estDossier = element.estDossier,
                taille = if (element.estDossier) 0L else destCorbeille.length(),
                dateSuppression = System.currentTimeMillis()
            )
        )
        sauvegarderListe(context, actuelle)
        true
    }

    suspend fun restaurer(context: Context, item: ElementCorbeille): Boolean = withContext(Dispatchers.IO) {
        val srcCorbeille = File(item.cheminStockageCorbeille)
        if (!srcCorbeille.exists()) return@withContext false

        val cible = File(item.cheminOriginal)
        val destinationFinale = if (cible.exists()) {
            val parent = cible.parentFile ?: File(UnifiedFileEngine.racineStockage)
            val base = cible.nameWithoutExtension
            val ext = if (cible.extension.isNotEmpty()) ".${cible.extension}" else ""
            File(parent, "${base}_restaure_${System.currentTimeMillis()}$ext")
        } else {
            cible.parentFile?.mkdirs()
            cible
        }

        val sourceDest = UnifiedFileEngine.determinerSource(destinationFinale.absolutePath)
        val ok = when (sourceDest) {
            SourceFichier.STANDARD -> {
                srcCorbeille.renameTo(destinationFinale) ||
                    (srcCorbeille.copyRecursively(destinationFinale, true) && srcCorbeille.deleteRecursively())
            }
            SourceFichier.SHIZUKU -> {
                ShizukuManager.renommer(srcCorbeille.absolutePath, destinationFinale.absolutePath) ||
                    (ShizukuManager.copier(srcCorbeille.absolutePath, destinationFinale.absolutePath) && srcCorbeille.deleteRecursively())
            }
            SourceFichier.ROOT -> {
                RootManager.renommer(srcCorbeille.absolutePath, destinationFinale.absolutePath) ||
                    (RootManager.copier(srcCorbeille.absolutePath, destinationFinale.absolutePath) && srcCorbeille.deleteRecursively())
            }
        }

        if (ok) {
            val restante = listerCorbeille(context).filterNot { it.id == item.id }
            sauvegarderListe(context, restante)
        }
        ok
    }

    suspend fun supprimerDefinitivement(context: Context, item: ElementCorbeille): Boolean = withContext(Dispatchers.IO) {
        val f = File(item.cheminStockageCorbeille)
        val ok = if (!f.exists()) true else if (f.isDirectory) f.deleteRecursively() else f.delete()
        if (ok) {
            val restante = listerCorbeille(context).filterNot { it.id == item.id }
            sauvegarderListe(context, restante)
        }
        ok
    }

    suspend fun viderCorbeille(context: Context) = withContext(Dispatchers.IO) {
        val dir = dossierCorbeille()
        dir.listFiles()?.forEach { f ->
            if (f.isDirectory) f.deleteRecursively() else f.delete()
        }
        sauvegarderListe(context, emptyList())
    }
}
