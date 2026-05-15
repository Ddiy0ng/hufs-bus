package com.hufsteam.shuttletrack.data.model

/**
 * 앱 사용자 역할 구분
 * - STUDENT : 일반 학생 (셔틀 위치 조회, 알림 수신)
 * - DRIVER  : 버스 기사 (위치 전송, NFC 탑승 집계)
 * - ADMIN   : 관리자 (노선·버스·기사 관리)
 */
enum class UserRole {
    STUDENT,
    DRIVER,
    ADMIN
}
