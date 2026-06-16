package com.hufsteam.shuttletrack.data.model

/**
 * Firestore "users/{uid}" 문서와 매핑되는 데이터 클래스
 *
 * Firestore 저장 구조:
 * users/
 *   {uid}/
 *     email  : String
 *     name   : String
 *     role   : "STUDENT" | "DRIVER" | "ADMIN"
 *     fcmToken: String  (푸시 알림용 FCM 토큰)
 */
data class User(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val role: String = UserRole.STUDENT.name,
    val fcmToken: String = ""
)
