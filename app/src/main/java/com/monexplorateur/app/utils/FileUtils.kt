package com.monexplorateur.app.utils

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object FileUtils {

    fun compresserEnZip(source: File, destinationZip: File): Boolean {
        return try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(destinationZip))).use { zos ->
                if (source.isDirectory) {
                    ajouterDossierAuZip(zos, source, source.name)
                } else {
                    ajouterFichierAuZip(zos, source, source.name)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun ajouterDossierAuZip(zos: ZipOutputStream, dossier: File, cheminBase: String) {
        val enfants = dossier.listFiles() ?: return
        if (enfants.isEmpty()) {
            zos.putNextEntry(ZipEntry("$cheminBase/"))
            zos.closeEntry()
            return
        }
        for (enfant in enfants) {
            val nomEntree = "$cheminBase/${enfant.name}"
            if (enfant.isDirectory) {
                ajouterDossierAuZip(zos, enfant, nomEntree)
            } else {
                ajouterFichierAuZip(zos, enfant, nomEntree)
            }
        }
    }

    private fun ajouterFichierAuZip(zos: ZipOutputStream, fichier: File, nomEntree: String) {
        BufferedInputStream(FileInputStream(fichier)).use { bis ->
            zos.putNextEntry(ZipEntry(nomEntree))
            bis.copyTo(zos, bufferSize = 8192)
            zos.closeEntry()
        }
    }

    fun decompresserZip(fichierZip: File, dossierCible: File): Boolean {
        return try {
            dossierCible.mkdirs()
            val cheminCanoniqueCible = dossierCible.canonicalPath + File.separator
            ZipInputStream(BufferedInputStream(FileInputStream(fichierZip))).use { zis ->
                var entree = zis.nextEntry
                while (entree != null) {
                    val nouveauFichier = File(dossierCible, entree.name)
                    // Protection contre la faille Zip Slip
                    if (!nouveauFichier.canonicalPath.startsWith(cheminCanoniqueCible)) {
                        throw SecurityException("Entrée ZIP non autorisée : ${entree.name}")
                    }
                    if (entree.isDirectory) {
                        nouveauFichier.mkdirs()
                    } else {
                        nouveauFichier.parentFile?.mkdirs()
                        FileOutputStream(nouveauFichier).use { fos ->
                            zis.copyTo(fos, bufferSize = 8192)
                        }
                    }
                    zis.closeEntry()
                    entree = zis.nextEntry
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun calculerHash(fichier: File, algorithme: String): String {
        return try {
            if (!fichier.isFile) return "N/A (Dossier)"
            val digest = MessageDigest.getInstance(algorithme)
            FileInputStream(fichier).use { fis ->
                val buffer = ByteArray(8192)
                var lu: Int
                while (fis.read(buffer).also { lu = it } != -1) {
                    digest.update(buffer, 0, lu)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            "Erreur de calcul"
        }
    }
}
