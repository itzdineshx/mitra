package com.mitra.engine.access

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Manages the active task execution, Stop overlay connection, and persistent Stop notification.
 */
object TaskRunner {
    private const val CHANNEL_ID = "mitra_task_channel"
    private const val NOTIFICATION_ID = 1001

    @Volatile
    var currentJob: Job? = null
        private set

    @Volatile
    var engine: AccessibilityEngine? = null

    fun startTask(
        context: Context,
        engine: AccessibilityEngine,
        block: suspend CoroutineScope.() -> Unit,
    ): Job {
        cancelTask("starting new task")
        this.engine = engine
        showNotification(context)

        val job =
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    block()
                } finally {
                    hideNotification(context)
                }
            }
        currentJob = job
        return job
    }

    fun cancelTask(reason: String): Long {
        val requestedNs = System.nanoTime()
        val job = currentJob
        job?.cancel()
        currentJob = null
        engine?.stop()
        val completedNs = System.nanoTime()
        val latencyMs = (completedNs - requestedNs) / 1_000_000.0
        Log.i("MitraStop", "STOP_EXEC: requested=$requestedNs, completed=$completedNs, latencyMs=$latencyMs, reason=$reason")
        return completedNs - requestedNs
    }

    fun showNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Mitra Active Task",
                    NotificationManager.IMPORTANCE_LOW,
                )
            nm.createNotificationChannel(channel)
        }

        val stopIntent =
            Intent("com.mitra.app.STOP_TASK").apply {
                setPackage(context.packageName)
            }
        val stopPending =
            PendingIntent.getBroadcast(
                context,
                0,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_close_clear_cancel)
                .setContentTitle("Mitra is running a task")
                .setContentText("Tap Stop anytime to halt the task")
                .setOngoing(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPending)
                .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    fun hideNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        nm.cancel(NOTIFICATION_ID)
    }
}
