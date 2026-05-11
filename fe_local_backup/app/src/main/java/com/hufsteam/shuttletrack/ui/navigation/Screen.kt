package com.hufsteam.shuttletrack.ui.navigation

/**
 * 앱 내 모든 화면의 경로(route)를 한 곳에서 관리
 *
 * 화면 구조:
 *   splash        → 자동 로그인 상태 확인
 *   login         → 로그인
 *   signup        → 회원가입
 *   student_main  → 학생 메인 (실시간 셔틀 위치)
 *   driver_main   → 기사 메인 (위치 전송 / 탑승 집계)
 *   admin_main    → 관리자 메인 (노선·버스 관리)
 */
sealed class Screen(val route: String) {
    object Splash       : Screen("splash")
    object Login        : Screen("login")
    object SignUp       : Screen("signup")
    object StudentMain  : Screen("student_main")
    object DriverMain   : Screen("driver_main")
    object AdminMain    : Screen("admin_main")
}
