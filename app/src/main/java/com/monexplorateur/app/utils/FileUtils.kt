package com.monexplorateur.app.utils

import com.github.junrar.Junrar
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class FormatArchive(val extension: String, val libelle: String) {
    ZIP("zip", "ZIP (.zip — universel et rapide)"),
    SEVEN_Z("7z", "7-Zip (.7z — haute compression)"),
    TAR_GZ("tar.gz", "TAR.GZ (.tar.gz — archive Linux/Unix)")
}

object FileUtils {

    private val EXTENSIONS_ARCHIVES = setOf("zip", "7z", "rar", "tar", "gz", "tgz", "bz2", "xz")

    fun estArchiveSupportee(nomFichier: String): Boolean {
        val lower = nomFichier.lowercase(Locale.getDefault())
        if (lower.endsWith(".tar.gz") || lower.endsWith(".tar.xz") || lower.endsWith(".tar.bz2")) return true
        val ext = File(lower).extension
        return ext in EXTENSIONS_ARCHIVES
    }

    fun nomSansExtensionArchive(nomFichier: String): String {
        val lower = nomFichier.lowercase(Locale.getDefault())
        return when {
            lower.endsWith(".tar.gz") -> nomFichier.dropLast(7)
            lower.endsWith(".tar.xz") -> nomFichier.dropLast(7)
            lower.endsWith(".tar.bz2") -> nomFichier.dropLast(8)
            else -> File(nomFichier).nameWithoutExtension
        }
    }

    fun compresser(sources: List<File>, destination: File, format: FormatArchive): Boolean {
        return try {
            when (format) {
                FormatArchive.ZIP -> compresserEnZip(sources, destination)
                FormatArchive.SEVEN_Z -> compresserEn7z(sources, destination)
                FormatArchive.TAR_GZ -> compresserEnTarGz(sources, destination)
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun compresserEnZip(sources: List<File>, destinationZip: File): Boolean {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(destinationZip))).use { zos ->
            for (src in sources) {
                ajouterAuZip(zos, src, src.name)
            }
        }
        return true
    }

    private fun ajouterAuZip(zos: ZipOutputStream, fichier: File, chemin: String) {
        if (fichier.isDirectory) {
            val enfants = fichier.listFiles() ?: emptyArray()
            if (enfants.isEmpty()) {
                zos.putNextEntry(ZipEntry("$chemin/"))
                zos.closeEntry()
            } else {
                for (enfant in enfants) {
                    ajouterAuZip(zos, enfant, "$chemin/${enfant.name}")
                }
            }
        } else {
            BufferedInputStream(FileInputStream(fichier)).use { bis ->
                zos.putNextEntry(ZipEntry(chemin))
                bis.copyTo(zos, 8192)
                zos.closeEntry()
            }
        }
    }

    private fun compresserEn7z(sources: List<File>, destination7z: File): Boolean {
        SevenZOutputFile(destination7z).use { sevenZOut ->
            for (src in sources) {
                ajouterAu7z(sevenZOut, src, src.name)
            }
        }
        return true
    }

    private fun ajouterAu7z(sevenZOut: SevenZOutputFile, fichier: File, chemin: String) {
        if (fichier.isDirectory) {
            val entry = sevenZOut.createArchiveEntry(fichier, "$chemin/")
            sevenZOut.putArchiveEntry(entry)
            sevenZOut.closeArchiveEntry()
            fichier.listFiles()?.forEach { enfant ->
                ajouterAu7z(sevenZOut, enfant, "$chemin/${enfant.name}")
            }
        } else {
            val entry: SevenZArchiveEntry = sevenZOut.createArchiveEntry(fichier, chemin)
            sevenZOut.putArchiveEntry(entry)
            FileInputStream(fichier).use { fis ->
                val buffer = ByteArray(8192)
                var len: Int
                while (fis.read(buffer).also { len = it } > 0) {
                    sevenZOut.write(buffer, 0, len)
                }
            }
            sevenZOut.closeArchiveEntry()
        }
    }

    private fun compresserEnTarGz(sources: List<File>, destinationTarGz: File): Boolean {
        TarArchiveOutputStream(
            GzipCompressorOutputStream(BufferedOutputStream(FileOutputStream(destinationTarGz)))
        ).use { taos ->
            taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
            for (src in sources) {
                ajouterAuTar(taos, src, src.name)
            }
        }
        return true
    }

    private fun ajouterAuTar(taos: TarArchiveOutputStream, fichier: File, chemin: String) {
        val entry = TarArchiveEntry(fichier, if (fichier.isDirectory) "$chemin/" else chemin)
        taos.putArchiveEntry(entry)
        if (fichier.isFile) {
            BufferedInputStream(FileInputStream(fichier)).use { bis ->
                bis.copyTo(taos, 8192)
            }
        }
        taos.closeArchiveEntry()
        if (fichier.isDirectory) {
            fichier.listFiles()?.forEach { enfant ->
                ajouterAuTar(taos, enfant, "$chemin/${enfant.name}")
            }
        }
    }

    fun decompresser(archive: File, dossierCible: File): Boolean {
        return try {
            dossierCible.mkdirs()
            val lower = archive.name.lowercase(Locale.getDefault())
            when {
                lower.endsWith(".7z") -> decompresser7z(archive, dossierCible)
                lower.endsWith(".rar") -> decompresserRar(archive, dossierCible)
                lower.endsWith(".tar.gz") || lower.endsWith(".tgz") -> decompresserTarGz(archive, dossierCible)
                else -> decompresserGenerique(archive, dossierCible)
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun decompresser7z(archive: File, dossierCible: File): Boolean {
        val baseCanon = dossierCible.canonicalPath + File.separator
        SevenZFile.builder().setFile(archive).get().use { sevenZFile ->
            var entry = sevenZFile.nextEntry
            while (entry != null) {
                val outFile = File(dossierCible, entry.name)
                if (!outFile.canonicalPath.startsWith(baseCanon)) throw SecurityException("Zip Slip")
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        val buffer = ByteArray(8192)
                        var len: Int
                        while (sevenZFile.read(buffer).also { len = it } > 0) {
                            fos.write(buffer, 0, len)
                        }
                    }
                }
                entry = sevenZFile.nextEntry
            }
        }
        return true
    }

    private fun decompresserRar(archive: File, dossierCible: File): Boolean {
        Junrar.extract(archive, dossierCible)
        return true
    }

    private fun decompresserTarGz(archive: File, dossierCible: File): Boolean {
        val baseCanon = dossierCible.canonicalPath + File.separator
        ArchiveStreamFactory().createArchiveInputStream(
            ArchiveStreamFactory.TAR,
            GzipCompressorInputStream(BufferedInputStream(FileInputStream(archive)))
        ).use { ais ->
            var entry: ArchiveEntry? = ais.nextEntry
            while (entry != null) {
                val outFile = File(dossierCible, entry.name)
                if (!outFile.canonicalPath.startsWith(baseCanon)) throw SecurityException("Zip Slip")
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos -> ais.copyTo(fos, 8192) }
                }
                entry = ais.nextEntry
            }
        }
        return true
    }

    private fun decompresserGenerique(archive: File, dossierCible: File): Boolean {
        val baseCanon = dossierCible.canonicalPath + File.separator
        BufferedInputStream(FileInputStream(archive)).use { bis ->
            ArchiveStreamFactory().createArchiveInputStream(bis).use { ais ->
                var entry: ArchiveEntry? = ais.nextEntry
                while (entry != null) {
                    val outFile = File(dossierCible, entry.name)
                    if (!outFile.canonicalPath.startsWith(baseCanon)) throw SecurityException("Zip Slip")
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos -> ais.copyTo(fos, 8192) }
                    }
                    entry = ais.nextEntry
                }
            }
        }
        return true
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
