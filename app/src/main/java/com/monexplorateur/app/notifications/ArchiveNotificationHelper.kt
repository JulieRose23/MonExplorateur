package com.monexplorateur.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class ArchiveNotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "archive_extraction_channel"
        private const val BASE_NOTIFICATION_ID = 4010
    }

    init {
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Extraction d'archives",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Progression de la decompression des archives"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun canNotify(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showProgress(archiveName: String, progress: Int, indeterminate: Boolean = false, notifId: Int = BASE_NOTIFICATION_ID) {
        if (!canNotify()) return
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Extraction : $archiveName")
            .setContentText(if (indeterminate) "Decompression en cours..." else "$progress %")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress.coerceIn(0, 100), indeterminate)

        NotificationManagerCompat.from(context).notify(notifId, builder.build())
    }

    fun showCompleted(archiveName: String, success: Boolean, message: String? = null, notifId: Int = BASE_NOTIFICATION_ID) {
        if (!canNotify()) return
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(
                if (success) android.R.drawable.stat_sys_download_done
                else android.R.drawable.stat_notify_error
            )
            .setContentTitle(
                if (success) "Extraction terminee : $archiveName"
                else "Echec de l'extraction : $archiveName"
            )
            .setContentText(message ?: if (success) "Archive extraite avec succes" else "Erreur lors de l'extraction")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(false)
            .setAutoCancel(true)
            .setProgress(0, 0, false)

        NotificationManagerCompat.from(context).notify(notifId, builder.build())
    }
}
