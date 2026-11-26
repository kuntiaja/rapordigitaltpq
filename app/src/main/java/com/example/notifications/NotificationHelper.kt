package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {
    const val CHANNEL_DAILY = "rapor_tpq_daily"
    const val CHANNEL_WEEKLY = "rapor_tpq_weekly"
    const val CHANNEL_MONTHLY = "rapor_tpq_monthly"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_DAILY,
                    "Pengingat Setor Hafalan Harian",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Pengingat jadwal setor hafalan Al-Qur'an harian santri TPQ"
                },
                NotificationChannel(
                    CHANNEL_WEEKLY,
                    "Progres Mingguan Otomatis",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Ringkasan perkembangan hafalan, tajwid, dan kehadiran mingguan"
                },
                NotificationChannel(
                    CHANNEL_MONTHLY,
                    "Laporan Bulanan Wali Murid",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifikasi penerbitan laporan bulanan santri kepada wali murid"
                }
            )
            channels.forEach { nm.createNotificationChannel(it) }
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun sendNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        body: String
    ): Boolean {
        ensureChannels(context)
        if (!hasNotificationPermission(context)) return false

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
