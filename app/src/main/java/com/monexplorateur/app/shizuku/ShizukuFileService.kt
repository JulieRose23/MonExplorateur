package com.monexplorateur.app.shizuku

import com.monexplorateur.app.IShizukuFileService
import java.io.File
import kotlin.system.exitProcess

class ShizukuFileService : IShizukuFileService.Stub() {

    override fun destroy() {
        exitProcess(0)
    }

    override fun listFiles(path: String): String {
        val dir = File(path)
        val children = dir.listFiles() ?: return ""
        return children.joinToString("\n") { file ->
            val isDir = if (file.isDirectory) "1" else "0"
            val size = if (file.isDirectory) (file.listFiles()?.size?.toLong() ?: 0L) else file.length()
            val mod = file.lastModified()
            // Format : isDir|size|lastModified|name
            "$isDir|$size|$mod|${file.name}"
        }
    }

    override fun createDirectory(path: String): Boolean {
        return try {
            File(path).mkdirs()
        } catch (e: Exception) {
            false
        }
    }

    override fun deletePath(path: String): Boolean {
        return try {
            val target = File(path)
            if (target.isDirectory) target.deleteRecursively() else target.delete()
        } catch (e: Exception) {
            false
        }
    }

    override fun renamePath(oldPath: String, newPath: String): Boolean {
        return try {
            File(oldPath).renameTo(File(newPath))
        } catch (e: Exception) {
            false
        }
    }

    override fun copyPath(srcPath: String, destPath: String): Boolean {
        return try {
            File(srcPath).copyRecursively(File(destPath), overwrite = false)
        } catch (e: Exception) {
            false
        }
    }

    override fun copyToLocalCache(srcPath: String, cacheDestPath: String): Boolean {
        return try {
            val src = File(srcPath)
            val dest = File(cacheDestPath)
            dest.parentFile?.mkdirs()
            src.copyTo(dest, overwrite = true)
            true
        } catch (e: Exception) {
            false
        }
    }
}
