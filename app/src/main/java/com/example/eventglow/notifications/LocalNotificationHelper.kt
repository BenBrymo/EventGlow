package com.example.eventglow.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.eventglow.MainActivity
import com.example.eventglow.R

object LocalNotificationHelper {

    private const val CHANNEL_ID = "eventglow_general_channel"

    fun show(
        context: Context,
        title: String,
        body: String,
        route: String? = null,
        eventId: String? = null
    ) {
        createChannelIfNeeded(context)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (!route.isNullOrBlank()) {
                putExtra(NotificationDeepLinkStore.EXTRA_ROUTE, route)
            }
            if (!eventId.isNullOrBlank()) {
                putExtra(NotificationDeepLinkStore.EXTRA_EVENT_ID, eventId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "General Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "General app notifications for EventGlow"
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }
}

