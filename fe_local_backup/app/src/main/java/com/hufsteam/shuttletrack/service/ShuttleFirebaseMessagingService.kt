package com.hufsteam.shuttletrack.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.hufsteam.shuttletrack.MainActivity

/**
 * FCM 푸시 알림 수신 서비스
 *
 * 역할:
 * 1. onNewToken  - FCM 토큰이 새로 발급될 때 Firestore에 저장
 * 2. onMessageReceived - 메시지 수신 시 상태바 알림으로 표시
 *
 * 알림 채널 ID:
 * - SHUTTLE_ALERT : 셔틀 출발/도착 알림
 */
class ShuttleFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID   = "SHUTTLE_ALERT"
        const val CHANNEL_NAME = "셔틀 알림"
    }

    // ──────────────────────────────────────────────────
    // FCM 토큰 갱신 시 Firestore에 저장
    // ──────────────────────────────────────────────────
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = Firebase.auth.currentUser?.uid ?: return
        Firebase.firestore.collection("users").document(uid)
            .update("fcmToken", token)
    }

    // ──────────────────────────────────────────────────
    // 메시지 수신 → 상태바 알림 표시
    // ──────────────────────────────────────────────────
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: "외대 셔틀"
        val body  = message.notification?.body  ?: ""

        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 알림 채널 생성 (Android 8.0+)
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(channel)

        // 알림 탭 시 MainActivity 실행
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
