package com.hufsteam.shuttletrack.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

private const val NOTIF_LOG_TAG = "NotificationScheduler"
const val NOTIFY_MINUTES_BEFORE = 5

data class ScheduledNotification(
    val scheduleId: Int,
    val routeName: String,
    val departureTime: String,
    val days: Set<String>
)

object NotificationScheduler {

    fun scheduleFavoriteNotifications(
        context: Context,
        notifications: List<ScheduledNotification>
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelAll(context, alarmManager, notifications)
        notifications.forEach { notification ->
            notification.days.forEach { day ->
                scheduleAlarm(context, alarmManager, notification, day)
            }
        }
        Log.i(NOTIF_LOG_TAG, "scheduled ${notifications.sumOf { it.days.size }} alarms for ${notifications.size} favorites")
    }

    private fun cancelAll(
        context: Context,
        alarmManager: AlarmManager,
        notifications: List<ScheduledNotification>
    ) {
        notifications.forEach { notification ->
            notification.days.forEach { day ->
                val pi = buildPendingIntent(context, notification, day, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
                pi?.let { alarmManager.cancel(it) }
            }
        }
    }

    private fun scheduleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        notification: ScheduledNotification,
        day: String
    ) {
        val timeParts = notification.departureTime.split(":")
        val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: return
        val minute = (timeParts.getOrNull(1)?.toIntOrNull() ?: 0) - NOTIFY_MINUTES_BEFORE

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, if (minute < 0) hour - 1 else hour)
            set(Calendar.MINUTE, if (minute < 0) 60 + minute else minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val targetDow = dayStringToCalendarDay(day)
        if (targetDow != null) {
            val currentDow = calendar.get(Calendar.DAY_OF_WEEK)
            var daysUntil = (targetDow - currentDow + 7) % 7
            if (daysUntil == 0 && calendar.timeInMillis <= System.currentTimeMillis()) daysUntil = 7
            calendar.add(Calendar.DAY_OF_MONTH, daysUntil)
        } else {
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val pi = buildPendingIntent(context, notification, day, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            ?: return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pi)
                Log.i(NOTIF_LOG_TAG, "inexact alarm: id=${notification.scheduleId} day=$day at=${calendar.time}")
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pi)
                Log.i(NOTIF_LOG_TAG, "exact alarm: id=${notification.scheduleId} day=$day at=${calendar.time}")
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pi)
            Log.w(NOTIF_LOG_TAG, "SecurityException, fallback inexact: ${e.message}")
        }
    }

    private fun buildPendingIntent(
        context: Context,
        notification: ScheduledNotification,
        day: String,
        flags: Int
    ): PendingIntent? {
        val intent = Intent(context, DepartureAlarmReceiver::class.java).apply {
            putExtra(DepartureAlarmReceiver.EXTRA_ROUTE_NAME, notification.routeName)
            putExtra(DepartureAlarmReceiver.EXTRA_DEPARTURE_TIME, notification.departureTime)
            putExtra(DepartureAlarmReceiver.EXTRA_SCHEDULE_ID, notification.scheduleId)
            putExtra(DepartureAlarmReceiver.EXTRA_DAY, day)
        }
        val requestCode = notification.scheduleId * 10 + dayIndex(day)
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    private fun dayIndex(day: String): Int = when (day) {
        "월요일" -> 1; "화요일" -> 2; "수요일" -> 3; "목요일" -> 4; "금요일" -> 5; else -> 0
    }

    private fun dayStringToCalendarDay(day: String): Int? = when (day) {
        "월요일" -> Calendar.MONDAY
        "화요일" -> Calendar.TUESDAY
        "수요일" -> Calendar.WEDNESDAY
        "목요일" -> Calendar.THURSDAY
        "금요일" -> Calendar.FRIDAY
        else -> null
    }
}
