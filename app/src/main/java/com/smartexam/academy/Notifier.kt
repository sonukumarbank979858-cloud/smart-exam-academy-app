package com.smartexam.academy

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
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.smartexam.academy.data.Repo
import java.util.concurrent.TimeUnit

object Notifier {
    const val CHANNEL = "sea_updates"
    private const val WORK = "sea_notification_poll"

    fun createChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL, "टेस्ट और परिणाम", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "नए टेस्ट, आपके परिणाम और घोषणाएँ"
            }
            ctx.getSystemService(NotificationManager::class.java)?.createNotificationChannel(ch)
        }
    }

    /** 15-minute background check for new notifications. */
    fun schedule(ctx: Context) {
        val req = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, req)
    }

    fun cancel(ctx: Context) = WorkManager.getInstance(ctx).cancelUniqueWork(WORK)

    fun canPost(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun show(ctx: Context, id: Int, title: String, body: String) {
        if (!canPost(ctx)) return
        createChannel(ctx)
        val open = PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra("open_notifications", true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setColor(0xFF1B1F5E.toInt())
            .setAutoCancel(true)
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try { NotificationManagerCompat.from(ctx).notify(id, n) } catch (_: SecurityException) {}
    }
}

class NotificationWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val repo = Repo(applicationContext)
        if (!repo.supa.session.isLoggedIn) return Result.success()
        return try {
            val list = repo.notifications(20)
            val last = repo.prefs.lastNotified
            val fresh = list.filter { !it.isRead && (last.isBlank() || it.createdAt > last) }
            if (last.isBlank()) {
                // first run: just remember where we are, don't flood old items
                list.firstOrNull()?.let { repo.prefs.lastNotified = it.createdAt }
            } else {
                fresh.take(5).reversed().forEach { Notifier.show(applicationContext, it.id.hashCode(), it.title, it.body) }
                list.maxByOrNull { it.createdAt }?.let { if (it.createdAt > last) repo.prefs.lastNotified = it.createdAt }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
