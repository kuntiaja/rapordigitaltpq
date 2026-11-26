package com.example.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DailySetorWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        NotificationHelper.sendNotification(
            context = applicationContext,
            channelId = NotificationHelper.CHANNEL_DAILY,
            notificationId = 1001,
            title = "Pengingat Setor Hafalan TPQ",
            body = "Waktunya persiapan setor hafalan & muroja'ah Al-Qur'an hari ini. Jangan lupa catat kehadiran dan nilai tajwid santri!"
        )
        return Result.success()
    }
}

class WeeklyProgressWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        NotificationHelper.sendNotification(
            context = applicationContext,
            channelId = NotificationHelper.CHANNEL_WEEKLY,
            notificationId = 1002,
            title = "Ringkasan Progres Mingguan TPQ",
            body = "Laporan mingguan hafalan, tajwid, dan persentase kehadiran santri telah diperbarui. Cek dashboard statistik untuk melihat perkembangan terbaru."
        )
        return Result.success()
    }
}

class MonthlyReportWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        NotificationHelper.sendNotification(
            context = applicationContext,
            channelId = NotificationHelper.CHANNEL_MONTHLY,
            notificationId = 1003,
            title = "Laporan Perkembangan Bulanan Siap",
            body = "Rapor bulanan hafalan & tajwid TPQ telah dirangkum otomatis untuk diteruskan kepada wali murid."
        )
        return Result.success()
    }
}

object TpqWorkScheduler {
    private const val DAILY_WORK_NAME = "tpq_daily_setor_reminder"
    private const val WEEKLY_WORK_NAME = "tpq_weekly_progress_digest"
    private const val MONTHLY_WORK_NAME = "tpq_monthly_auto_report"

    fun syncSchedules(
        context: Context,
        dailyEnabled: Boolean,
        dailyHour: Int,
        dailyMinute: Int,
        weeklyEnabled: Boolean,
        monthlyEnabled: Boolean
    ) {
        val wm = WorkManager.getInstance(context)

        if (dailyEnabled) {
            val initialDelay = computeDelayUntilNext(dailyHour, dailyMinute)
            val dailyReq = PeriodicWorkRequestBuilder<DailySetorWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()
            wm.enqueueUniquePeriodicWork(
                DAILY_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                dailyReq
            )
        } else {
            wm.cancelUniqueWork(DAILY_WORK_NAME)
        }

        if (weeklyEnabled) {
            val weeklyReq = PeriodicWorkRequestBuilder<WeeklyProgressWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(7, TimeUnit.DAYS)
                .build()
            wm.enqueueUniquePeriodicWork(
                WEEKLY_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                weeklyReq
            )
        } else {
            wm.cancelUniqueWork(WEEKLY_WORK_NAME)
        }

        if (monthlyEnabled) {
            val monthlyReq = PeriodicWorkRequestBuilder<MonthlyReportWorker>(30, TimeUnit.DAYS)
                .setInitialDelay(30, TimeUnit.DAYS)
                .build()
            wm.enqueueUniquePeriodicWork(
                MONTHLY_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                monthlyReq
            )
        } else {
            wm.cancelUniqueWork(MONTHLY_WORK_NAME)
        }
    }

    private fun computeDelayUntilNext(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return (target.timeInMillis - now.timeInMillis).coerceAtLeast(60_000L)
    }
}
