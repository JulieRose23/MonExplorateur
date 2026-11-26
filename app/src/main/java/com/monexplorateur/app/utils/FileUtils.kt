package com.monexplorateur.app.utils

import com.github.junrar.Junrar
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.EncryptionMethod
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
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

enum class FormatArchive(val extension: String, val libelleFr: String, val libelleEn: String) {
    ZIP("zip", "ZIP (.zip — universel, supporte mot de passe)", "ZIP (.zip — universal, supports password)"),
    SEVEN_Z("7z", "7-Zip (.7z — haute compression)", "7-Zip (.7z — high compression)"),
    TAR_GZ("tar.gz", "TAR.GZ (.tar.gz — archive Linux/Unix)", "TAR.GZ (.tar.gz — Linux/Unix archive)");

    fun libelle(en: Boolean): String = if (en) libelleEn else libelleFr
}

object FileUtils {

    private val EXTENSIONS_ARCHIVES = setOf("zip", "7z", "rar", "tar", "gz", "tgz", "bz2", "xz")
    private val REGEX_PARTIE = Regex("""^(.+\.(?:zip|7z|rar|tar|gz|tgz|bz2|xz|tar\.gz|tar\.xz|tar\.bz2))_(\d+)$""", RegexOption.IGNORE_CASE)

    fun estArchiveSupportee(nomFichier: String): Boolean {
        val nomBase = retirerSuffixePartie(nomFichier)
        val lower = nomBase.lowercase(Locale.getDefault())
        if (lower.endsWith(".tar.gz") || lower.endsWith(".tar.xz") || lower.endsWith(".tar.bz2")) return true
        val ext = File(lower).extension
        return ext in EXTENSIONS_ARCHIVES
    }

    fun estArchiveDecoupee(nomFichier: String): Boolean = REGEX_PARTIE.matches(nomFichier)

    fun retirerSuffixePartie(nomFichier: String): String {
        val match = REGEX_PARTIE.matchEntire(nomFichier)
        return match?.groupValues?.get(1) ?: nomFichier
    }

    fun nomSansExtensionArchive(nomFichier: String): String {
        val nomBase = retirerSuffixePartie(nomFichier)
        val lower = nomBase.lowercase(Locale.getDefault())
        return when {
            lower.endsWith(".tar.gz") -> nomBase.dropLast(7)
            lower.endsWith(".tar.xz") -> nomBase.dropLast(7)
            lower.endsWith(".tar.bz2") -> nomBase.dropLast(8)
            else -> File(nomBase).nameWithoutExtension
        }
    }

    private fun compterFichiers(sources: List<File>): Int {
        var total = 0
        for (src in sources) {
            if (src.isDirectory) {
                src.walkTopDown().forEach { if (it.isFile) total++ }
            } else if (src.isFile) {
                total++
            }
        }
        return total.coerceAtLeast(1)
    }

    fun compresser(
        sources: List<File>,
        destination: File,
        format: FormatArchive,
        motDePasse: String? = null,
        tailleSplitMo: Long? = null,
        onProgress: ((Int) -> Unit)? = null
    ): Boolean {
        return try {
            val totalFichiers = compterFichiers(sources)
            var traites = 0
            val maxPctAvantSplit = if (tailleSplitMo != null && tailleSplitMo > 0L) 85 else 99
            val tick: () -> Unit = {
                traites++
                onProgress?.invoke(((traites * maxPctAvantSplit) / totalFichiers).coerceIn(1, maxPctAvantSplit))
            }

            val formatEffectif = if (!motDePasse.isNullOrEmpty()) FormatArchive.ZIP else format
            val fichierCible = if (!motDePasse.isNullOrEmpty() && format != FormatArchive.ZIP) {
                File(destination.parentFile, "${destination.nameWithoutExtension}.zip")
            } else {
                destination
            }

            if (fichierCible.exists()) fichierCible.delete()

            val ok = when {
                !motDePasse.isNullOrEmpty() -> compresserEnZipChiffre(sources, fichierCible, motDePasse, tick)
                formatEffectif == FormatArchive.ZIP -> compresserEnZip(sources, fichierCible, tick)
                formatEffectif == FormatArchive.SEVEN_Z -> compresserEn7z(sources, fichierCible, tick)
                else -> compresserEnTarGz(sources, fichierCible, tick)
            }

            if (!ok || !fichierCible.exists()) return false

            if (tailleSplitMo != null && tailleSplitMo > 0L) {
                val tailleOctets = tailleSplitMo * 1024L * 1024L
                if (fichierCible.length() > tailleOctets) {
                    decouperFichier(fichierCible, tailleOctets) { pctSplit ->
                        onProgress?.invoke((85 + (pctSplit * 14) / 100).coerceIn(85, 99))
                    }
                    fichierCible.delete()
                }
            }

            onProgress?.invoke(100)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun decouperFichier(source: File, tailleMaxOctets: Long, onProgress: ((Int) -> Unit)?) {
        val total = source.length().coerceAtLeast(1L)
        var lusTotal = 0L
        var indexPartie = 1
        val buffer = ByteArray(65536)

        BufferedInputStream(FileInputStream(source)).use { bis ->
            while (true) {
                val premierOctet = bis.read()
                if (premierOctet == -1) break

                val fichierPartie = File(source.parentFile, "${source.name}_$indexPartie")
                BufferedOutputStream(FileOutputStream(fichierPartie)).use { bos ->
                    bos.write(premierOctet)
                    var ecritsDansPartie = 1L
                    lusTotal += 1L

                    while (ecritsDansPartie < tailleMaxOctets) {
                        val aLire = minOf(buffer.size.toLong(), tailleMaxOctets - ecritsDansPartie).toInt()
                        val n = bis.read(buffer, 0, aLire)
                        if (n <= 0) break
                        bos.write(buffer, 0, n)
                        ecritsDansPartie += n
                        lusTotal += n
                        onProgress?.invoke(((lusTotal * 100) / total).toInt().coerceIn(1, 99))
                    }
                }
                indexPartie++
            }
        }
    }

    private fun compresserEnZipChiffre(
        sources: List<File>,
        destinationZip: File,
        motDePasse: String,
        onFileDone: (() -> Unit)?
    ): Boolean {
        val zipFile = ZipFile(destinationZip, motDePasse.toCharArray())
        val params = ZipParameters().apply {
            isEncryptFiles = true
            encryptionMethod = EncryptionMethod.AES
            aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
        }
        for (src in sources) {
            if (src.isDirectory) {
                zipFile.addFolder(src, params)
                src.walkTopDown().forEach { if (it.isFile) onFileDone?.invoke() }
            } else if (src.isFile) {
                zipFile.addFile(src, params)
                onFileDone?.invoke()
            }
        }
        return true
    }
    fun compresserEnZip(sources: List<File>, destinationZip: File, onFileDone: (() -> Unit)? = null): Boolean {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(destinationZip))).use { zos ->
            for (src in sources) ajouterAuZip(zos, src, src.name, onFileDone)
        }
        return true
    }

    private fun ajouterAuZip(zos: ZipOutputStream, fichier: File, chemin: String, onFileDone: (() -> Unit)?) {
        if (fichier.isDirectory) {
            val enfants = fichier.listFiles() ?: emptyArray()
            if (enfants.isEmpty()) {
                zos.putNextEntry(ZipEntry("$chemin/"))
                zos.closeEntry()
            } else {
                for (enfant in enfants) ajouterAuZip(zos, enfant, "$chemin/${enfant.name}", onFileDone)
            }
        } else {
            BufferedInputStream(FileInputStream(fichier)).use { bis ->
                zos.putNextEntry(ZipEntry(chemin))
                bis.copyTo(zos, 8192)
                zos.closeEntry()
            }
            onFileDone?.invoke()
        }
    }

    private fun compresserEn7z(sources: List<File>, destination7z: File, onFileDone: (() -> Unit)? = null): Boolean {
        SevenZOutputFile(destination7z).use { sevenZOut ->
            for (src in sources) ajouterAu7z(sevenZOut, src, src.name, onFileDone)
        }
        return true
    }

    private fun ajouterAu7z(sevenZOut: SevenZOutputFile, fichier: File, chemin: String, onFileDone: (() -> Unit)?) {
        if (fichier.isDirectory) {
            val entry = sevenZOut.createArchiveEntry(fichier, "$chemin/")
            sevenZOut.putArchiveEntry(entry)
            sevenZOut.closeArchiveEntry()
            fichier.listFiles()?.forEach { enfant -> ajouterAu7z(sevenZOut, enfant, "$chemin/${enfant.name}", onFileDone) }
        } else {
            val entry: SevenZArchiveEntry = sevenZOut.createArchiveEntry(fichier, chemin)
            sevenZOut.putArchiveEntry(entry)
            FileInputStream(fichier).use { fis ->
                val buffer = ByteArray(8192)
                var len: Int
                while (fis.read(buffer).also { len = it } > 0) sevenZOut.write(buffer, 0, len)
            }
            sevenZOut.closeArchiveEntry()
            onFileDone?.invoke()
        }
    }

    private fun compresserEnTarGz(sources: List<File>, destinationTarGz: File, onFileDone: (() -> Unit)? = null): Boolean {
        TarArchiveOutputStream(
            GzipCompressorOutputStream(BufferedOutputStream(FileOutputStream(destinationTarGz)))
        ).use { taos ->
            taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
            for (src in sources) ajouterAuTar(taos, src, src.name, onFileDone)
        }
        return true
    }

    private fun ajouterAuTar(taos: TarArchiveOutputStream, fichier: File, chemin: String, onFileDone: (() -> Unit)?) {
        val entry = TarArchiveEntry(fichier, if (fichier.isDirectory) "$chemin/" else chemin)
        taos.putArchiveEntry(entry)
        if (fichier.isFile) {
            BufferedInputStream(FileInputStream(fichier)).use { bis -> bis.copyTo(taos, 8192) }
            onFileDone?.invoke()
        }
        taos.closeArchiveEntry()
        if (fichier.isDirectory) {
            fichier.listFiles()?.forEach { enfant -> ajouterAuTar(taos, enfant, "$chemin/${enfant.name}", onFileDone) }
        }
    }

    fun decompresser(
        archive: File,
        dossierCible: File,
        motDePasse: String? = null,
        onProgress: ((Int) -> Unit)? = null
    ): Boolean {
        var fichierTempReassemble: File? = null
        return try {
            dossierCible.mkdirs()

            val archiveEffective = if (estArchiveDecoupee(archive.name)) {
                onProgress?.invoke(5)
                val nomBase = retirerSuffixePartie(archive.name)
                val dossierParent = archive.parentFile ?: return false
                val parties = mutableListOf<File>()
                var idx = 1
                while (true) {
                    val f = File(dossierParent, "${nomBase}_$idx")
                    if (!f.exists()) break
                    parties.add(f)
                    idx++
                }
                if (parties.isEmpty()) return false
                val temp = File(dossierCible, ".tmp_reassembled_$nomBase")
                BufferedOutputStream(FileOutputStream(temp)).use { bos ->
                    for (p in parties) {
                        BufferedInputStream(FileInputStream(p)).use { bis -> bis.copyTo(bos, 65536) }
                    }
                }
                fichierTempReassemble = temp
                onProgress?.invoke(20)
                temp
            } else {
                archive
            }

            val lower = retirerSuffixePartie(archiveEffective.name).lowercase(Locale.getDefault())
            val ok = when {
                lower.endsWith(".zip") -> decompresserZip(archiveEffective, dossierCible, motDePasse, onProgress)
                lower.endsWith(".7z") -> decompresser7z(archiveEffective, dossierCible, motDePasse, onProgress)
                lower.endsWith(".rar") -> decompresserRar(archiveEffective, dossierCible, onProgress)
                lower.endsWith(".tar.gz") || lower.endsWith(".tgz") -> decompresserTarGz(archiveEffective, dossierCible, onProgress)
                else -> decompresserGenerique(archiveEffective, dossierCible, onProgress)
            }
            if (ok) onProgress?.invoke(100)
            ok
        } catch (_: Exception) {
            false
        } finally {
            fichierTempReassemble?.delete()
        }
    }

    private fun decompresserZip(archive: File, dossierCible: File, motDePasse: String?, onProgress: ((Int) -> Unit)?): Boolean {
        val zipFile = ZipFile(archive)
        if (zipFile.isEncrypted) {
            if (motDePasse.isNullOrEmpty()) return false
            zipFile.setPassword(motDePasse.toCharArray())
        }
        onProgress?.invoke(35)
        zipFile.extractAll(dossierCible.absolutePath)
        onProgress?.invoke(95)
        return true
    }

    private fun decompresser7z(archive: File, dossierCible: File, motDePasse: String?, onProgress: ((Int) -> Unit)?): Boolean {
        val baseCanon = dossierCible.canonicalPath + File.separator
        val builder = SevenZFile.builder().setFile(archive)
        if (!motDePasse.isNullOrEmpty()) builder.setPassword(motDePasse.toCharArray())
        builder.get().use { sevenZFile ->
            val totalEntries = sevenZFile.entries.count().coerceAtLeast(1)
            var current = 0
            var entry = sevenZFile.nextEntry
            while (entry != null) {
                val outFile = File(dossierCible, entry.name)
                if (!outFile.canonicalPath.startsWith(baseCanon)) throw SecurityException("Zip Slip")
                if (entry.isDirectory) outFile.mkdirs()
                else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        val buffer = ByteArray(8192)
                        var len: Int
                        while (sevenZFile.read(buffer).also { len = it } > 0) fos.write(buffer, 0, len)
                    }
                }
                current++
                onProgress?.invoke(((current * 100) / totalEntries).coerceIn(1, 99))
                entry = sevenZFile.nextEntry
            }
        }
        return true
    }

    private fun decompresserRar(archive: File, dossierCible: File, onProgress: ((Int) -> Unit)?): Boolean {
        onProgress?.invoke(30)
        Junrar.extract(archive, dossierCible)
        onProgress?.invoke(95)
        return true
    }

    private fun decompresserTarGz(archive: File, dossierCible: File, onProgress: ((Int) -> Unit)?): Boolean {
        val baseCanon = dossierCible.canonicalPath + File.separator
        val totalBytes = archive.length().coerceAtLeast(1L)
        var extractedBytes = 0L
        val stream: ArchiveInputStream<ArchiveEntry> = ArchiveStreamFactory().createArchiveInputStream(
            ArchiveStreamFactory.TAR,
            GzipCompressorInputStream(BufferedInputStream(FileInputStream(archive)))
        )
        stream.use { ais ->
            var entry: ArchiveEntry? = ais.nextEntry
            while (entry != null) {
                val outFile = File(dossierCible, entry.name)
                if (!outFile.canonicalPath.startsWith(baseCanon)) throw SecurityException("Zip Slip")
                if (entry.isDirectory) outFile.mkdirs()
                else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos -> ais.copyTo(fos, 8192) }
                    extractedBytes += outFile.length()
                    onProgress?.invoke(((extractedBytes * 50) / totalBytes).toInt().coerceIn(5, 95))
                }
                entry = ais.nextEntry
            }
        }
        return true
    }

    private fun decompresserGenerique(archive: File, dossierCible: File, onProgress: ((Int) -> Unit)?): Boolean {
        val baseCanon = dossierCible.canonicalPath + File.separator
        val totalBytes = archive.length().coerceAtLeast(1L)
        var extractedBytes = 0L
        BufferedInputStream(FileInputStream(archive)).use { bis ->
            val stream: ArchiveInputStream<ArchiveEntry> = ArchiveStreamFactory().createArchiveInputStream(bis)
            stream.use { ais ->
                var entry: ArchiveEntry? = ais.nextEntry
                while (entry != null) {
                    val outFile = File(dossierCible, entry.name)
                    if (!outFile.canonicalPath.startsWith(baseCanon)) throw SecurityException("Zip Slip")
                    if (entry.isDirectory) outFile.mkdirs()
                    else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos -> ais.copyTo(fos, 8192) }
                        extractedBytes += outFile.length()
                        onProgress?.invoke(((extractedBytes * 70) / totalBytes).toInt().coerceIn(5, 95))
                    }
                    entry = ais.nextEntry
                }
            }
        }
        return true
    }

    fun calculerHash(fichier: File, algorithme: String): String {
        return try {
            if (!fichier.isFile) return "N/A"
            val digest = MessageDigest.getInstance(algorithme)
            FileInputStream(fichier).use { fis ->
                val buffer = ByteArray(8192)
                var lu: Int
                while (fis.read(buffer).also { lu = it } != -1) digest.update(buffer, 0, lu)
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            "Error"
        }
    }
}