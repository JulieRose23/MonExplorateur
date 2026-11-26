package com.monexplorateur.app.root

import android.content.Intent
import android.os.IBinder
import com.monexplorateur.app.IShizukuFileService
import com.topjohnwu.superuser.ipc.RootService
import java.io.File

class RootFileService : RootService() {

    private val binder = object : IShizukuFileService.Stub() {
        override fun destroy() {
            stopSelf()
        }

        override fun listFiles(path: String): String {
            val dir = File(path)
            val children = dir.listFiles() ?: return ""
            return children.joinToString("\n") { file ->
                val isDir = if (file.isDirectory) "1" else "0"
                val size = if (file.isDirectory) (file.listFiles()?.size?.toLong() ?: 0L) else file.length()
                val mod = file.lastModified()
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
                dest.setReadable(true, false)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    override fun onBind(intent: Intent): IBinder = binder
}
