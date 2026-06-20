package com.hufsteam.shuttletrack.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.hufsteam.shuttletrack.MainActivity

class DepartureAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_ROUTE_NAME = "route_name"
        const val EXTRA_DEPARTURE_TIME = "departure_time"
        const val EXTRA_SCHEDULE_ID = "schedule_id"
        const val EXTRA_DAY = "day"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val routeName = intent.getStringExtra(EXTRA_ROUTE_NAME) ?: "셔틀 버스"
        val departureTime = intent.getStringExtra(EXTRA_DEPARTURE_TIME) ?: ""

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            ShuttleFirebaseMessagingService.CHANNEL_ID,
            ShuttleFirebaseMessagingService.CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(channel)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, launchIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val body = if (departureTime.isNotBlank()) {
            "$routeName $departureTime 출발까지 ${NOTIFY_MINUTES_BEFORE}분 남았습니다"
        } else {
            "$routeName 출발까지 ${NOTIFY_MINUTES_BEFORE}분 남았습니다"
        }

        val notification = NotificationCompat.Builder(context, ShuttleFirebaseMessagingService.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("셔틀 출발 알림")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
